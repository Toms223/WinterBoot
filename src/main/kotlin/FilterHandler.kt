

import annotations.injection.Leaf
import org.http4k.core.Filter
import org.http4k.core.HttpHandler

import org.http4k.core.then
import java.lang.reflect.Method

class FilterHandler {
    private val objectInstantiator = ObjectInstantiator()
    fun get(seedMap: Map<String, Any>, branchList: List<Class<*>>): Filter{
        val branchObjectList = branchList.map { objectInstantiator.instantiateObject(it, seedMap) }
        val leafList = branchList.associate {
            Pair(branchObjectList[branchList.indexOf(it)], getLeafs(it))
        }

        return leafList.map {
            it.value.map {
                leaf -> createFilter(leaf,seedMap,it.key)
            }
        }.flatten().reduce { acc, filter -> filter.then(acc) }
    }

    private fun getLeafs(branch: Class<*>): List<Method>{
        return branch.declaredMethods.filter { method ->
            method.isAnnotationPresent(Leaf::class.java) && method.parameters.any { it.name.lowercase() == "next" }
        }
    }

    @SuppressWarnings
    private fun createFilter(method: Method, seedMap: Map<String, Any>, obj: Any): Filter{
        return Filter { next ->
            { request ->
                val parameters = method.parameters.mapNotNull  {
                    when(it.name.lowercase()) {
                        "next" -> next
                        else -> seedMap[it.name.lowercase()]
                    }
                }.toTypedArray()
                (method.invoke(obj,*parameters) as HttpHandler).invoke(request)
            }
        }
    }
}