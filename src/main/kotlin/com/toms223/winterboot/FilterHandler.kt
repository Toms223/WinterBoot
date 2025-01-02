package com.toms223.winterboot

import com.toms223.winterboot.annotations.injection.Leaf
import org.http4k.core.Filter
import org.http4k.core.HttpHandler

import org.http4k.core.then
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberFunctions

class FilterHandler {
    private val objectInstantiation = ObjectInstantiation()
    fun get(seedMap: Map<String, Any>, branchList: List<KClass<out Any>>): Filter{
        val branchObjectList = branchList.map { objectInstantiation.instantiateObject(it, seedMap) }
        val leafList = branchList.associate {
            Pair(branchObjectList[branchList.indexOf(it)], getLeafs(it))
        }

        return leafList.map {
            it.value.map {
                leaf -> createFilter(leaf,seedMap,it.key)
            }
        }.flatten().reduce { acc, filter -> filter.then(acc) }
    }

    private fun getLeafs(branch: KClass<out Any>): List<KFunction<*>>{
        return branch.memberFunctions.filter { function ->
            function.annotations.any { annotation -> annotation.annotationClass == Leaf::class }
        }
    }

    @SuppressWarnings
    private fun createFilter(function: KFunction<*>, seedMap: Map<String, Any>, obj: Any): Filter{
        return Filter { next ->
            { request ->
                val parameters = function.parameters.mapNotNull  {
                    when(it.name?.lowercase()) {
                        "next" -> next
                        else -> seedMap[it.name?.lowercase()]
                    }
                }.toTypedArray()
                (function.call(obj,*parameters) as HttpHandler).invoke(request)
            }
        }
    }
}