

import annotations.injection.Insect
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.Exception

class ExceptionHandler {
    private val objectInstantiator = ObjectInstantiator()
    fun get(seedMap: Map<String, Any>, pesticideList: List<Class<*>>): Filter{
        val objectInstantiator = pesticideList.map { objectInstantiator.instantiateObject(it,seedMap) }
        val insects = pesticideList.associate { pesticide ->
            Pair(objectInstantiator[pesticideList.indexOf(pesticide)],getInsects(pesticide))
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

    private fun getInsects(clazz: Class<*>): List<Method>{
        return clazz.declaredMethods.filter { method ->
            method.isAnnotationPresent(Insect::class.java)
        }
    }

    private fun constructResponse(method: Method, exception: Exception, seedMap: Map<String, Any>, obj: Any): Response{
        val parameters = method.parameters.mapNotNull {
            if(Exception::class.java.isAssignableFrom(it.type)){
                exception
            } else {
                seedMap[it.name.lowercase()]
            }
        }.toTypedArray()
        return (method.invoke(obj,*parameters) as Response)
    }
    private fun constructFilter(method: Method, seedMap: Map<String, Any>, obj: Any): Filter{
        return Filter {
            next -> {
            val methodException = method.getAnnotation(Insect::class.java).type.java
                try{
                    next(it)
                }  catch (e: Exception){
                    val exception = if(e is InvocationTargetException) e.targetException as Exception else e
                    if(exception.javaClass == methodException ){
                        constructResponse(method, exception, seedMap, obj)}
                    else if(exception.javaClass.superclass == methodException){
                        constructResponse(method, exception, seedMap, obj)
                    } else {
                        throw exception
                    }
                }
            }
        }
    }

}