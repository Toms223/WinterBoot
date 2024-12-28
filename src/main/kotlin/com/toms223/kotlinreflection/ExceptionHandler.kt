package com.toms223.kotlinreflection

import com.toms223.winterboot.annotations.injection.Insect
import com.toms223.winterboot.annotations.injection.Seed
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.Exception
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.superclasses

class ExceptionHandler {
    private val objectInstantiation = ObjectInstantiation()
    fun get(seedMap: Map<String, Any>, pesticideList: List<KClass<out Any>>): Filter{
        val objects = pesticideList.map { objectInstantiation.instantiateObject(it,seedMap) }
        val insects = pesticideList.associate { pesticide ->
            Pair(objects[pesticideList.indexOf(pesticide)],getInsects(pesticide))
        }
        return Filter {
            next -> {
               try {
                   next(it)
               } catch (e: Exception){
                   println("\u001b[31m" + e.message + "\u001b[0m")
                   Response(Status.INTERNAL_SERVER_ERROR).body("An internal error occurred")
               }
        }
        }.then(insects.map {
            it.value.map { method ->
                constructFilter(method, seedMap, it.key)
            }
        }.flatten().reduce { acc, filter -> filter.then(acc) })
    }

    private fun getInsects(kClass: KClass<out Any>): List<KFunction<*>>{
        return kClass.memberFunctions.filter { it.annotations.map{it::class}.contains(Insect::class) }
    }

    private fun constructResponse(function: KFunction<*>, exception: Exception, seedMap: Map<String, Any>, obj: Any): Response{
        val parameters = function.parameters.mapNotNull { parameter ->
            val parameterTypeOfParam = parameter.type.classifier as KClass<*>
            if(Exception::class.isSubclassOf(parameterTypeOfParam)){
                exception
            } else {
                seedMap[parameter.name?.lowercase()]
            }
        }.toTypedArray()
        return (function.call(obj,*parameters) as Response)
    }
    private fun constructFilter(function: KFunction<*>, seedMap: Map<String, Any>, obj: Any): Filter{
        return Filter {
            next -> {
                try{
                    next(it)
                } catch (e: Exception){
                    val exception = if(e is InvocationTargetException) e.targetException as Exception else e
                    val insect = function.annotations.first { it::class == Insect::class } as Insect
                    if(exception::class == insect.type){
                        constructResponse(function, exception, seedMap, obj)
                    } else if(exception::class.superclasses.contains(insect.type)){
                        constructResponse(function, exception, seedMap, obj)
                    } else {
                        throw exception
                    }
                }
            }
        }
    }

}