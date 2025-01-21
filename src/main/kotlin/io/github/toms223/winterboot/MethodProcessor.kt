package io.github.toms223.winterboot


import io.github.toms223.winterboot.annotations.mappings.DeleteMapping
import io.github.toms223.winterboot.annotations.mappings.GetMapping
import io.github.toms223.winterboot.annotations.mappings.PostMapping
import io.github.toms223.winterboot.annotations.mappings.PutMapping
import io.github.toms223.winterboot.processors.Parameterized
import io.github.toms223.winterboot.processors.Unparameterized
import org.http4k.routing.RoutingHttpHandler
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberFunctions
import org.http4k.core.Method as HttpMethod

class MethodProcessor {
    private val mappingToMethodMap = mapOf(
        Pair(GetMapping::class,HttpMethod.GET),
        Pair(PutMapping::class,HttpMethod.PUT),
        Pair(PostMapping::class,HttpMethod.POST),
        Pair(DeleteMapping::class,HttpMethod.DELETE),
    )
    fun methodsToRoutes(obj: Any, kClass: KClass<out Any>): List<RoutingHttpHandler>{
        val handlers = kClass.memberFunctions.mapNotNull { method ->
            val parameters = method.parameters
            val mapEntry = getMapEntry(method) ?: return@mapNotNull null
            val path = getPath(method.annotations.first { mapEntry.key == it.annotationClass }) ?: throw IllegalArgumentException("No path found")
            if(parameters.isEmpty()){
                Unparameterized.process(method, obj, mapEntry, path)
            } else {
                Parameterized.process(method, parameters, obj, mapEntry, path)
            }
        }
        if(handlers.isEmpty()){
            throw IllegalArgumentException("No mappings found")
        }
        return handlers
    }

    private fun getMapEntry(method: KFunction<*>): Map.Entry<KClass<out Annotation>, HttpMethod>? {
        return mappingToMethodMap.entries.firstOrNull {
            method.annotations.any { annotation -> annotation.annotationClass == it.key }
        }
    }


    private fun getPath(annotation: Annotation): String?{
        return when(annotation){
            is io.github.toms223.winterboot.annotations.mappings.GetMapping -> annotation.path
            is PutMapping -> annotation.path
            is PostMapping -> annotation.path
            is DeleteMapping -> annotation.path
            else -> null
        }
    }
}