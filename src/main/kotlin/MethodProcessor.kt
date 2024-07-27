import annotations.mappings.DeleteMapping
import annotations.mappings.GetMapping
import annotations.mappings.PostMapping
import annotations.mappings.PutMapping
import org.http4k.routing.RoutingHttpHandler
import processors.Parameterized
import processors.Unparameterized
import java.lang.reflect.Method
import org.http4k.core.Method as HttpMethod

class MethodProcessor {
    private val mappingToMethodMap = mapOf(
        Pair(GetMapping::class.java,HttpMethod.GET),
        Pair(PutMapping::class.java,HttpMethod.PUT),
        Pair(PostMapping::class.java,HttpMethod.POST),
        Pair(DeleteMapping::class.java,HttpMethod.DELETE),
    )
    fun methodsToRoutes(obj: Any, clazz: Class<*>): List<RoutingHttpHandler>{
        val handlers = clazz.declaredMethods.mapNotNull { method ->
            val parameters = getParameterized(method)
            val mapEntry = getMapEntry(method) ?: return@mapNotNull null
            val path = getPath(method.getAnnotation(mapEntry.key)) ?: throw IllegalArgumentException("No path found")
            if(parameters.isEmpty()){
                Unparameterized.process(method, obj, mapEntry, path)
            } else {
                Parameterized.process(method, obj, mapEntry, path)
            }
        }
        if(handlers.isEmpty()){
            throw IllegalArgumentException("No mappings found")
        }
        return handlers
    }

    private fun getMapEntry(method: Method): Map.Entry<Class<out Annotation>, HttpMethod>? {
        return mappingToMethodMap.entries.firstOrNull {
            method.isAnnotationPresent(it.key)
        }
    }



    private fun getParameterized(method: Method): List<Class<out Annotation>> {
        return method.parameterAnnotations.map {
            array -> array.toList().map {
                annotation ->  annotation.javaClass
            }
        }.flatten()
    }

    private fun <T> getPath(annotation: T): String?{
        return when(annotation){
            is GetMapping -> annotation.path
            is PutMapping -> annotation.path
            is PostMapping -> annotation.path
            is DeleteMapping -> annotation.path
            else -> null
        }
    }
}