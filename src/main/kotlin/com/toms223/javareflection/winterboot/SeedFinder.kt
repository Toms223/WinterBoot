package com.toms223.winterboot


import com.toms223.winterboot.annotations.injection.Seed
import java.lang.reflect.Field
import java.lang.reflect.Method

class SeedFinder {
    fun getSeeds(fruitList: List<Class<*>>): Map<String, Any>{
        val objectList = fruitList.map { it.getDeclaredConstructor().newInstance() }
        val seedFields = fruitList.map { fruit ->
            val obj = objectList[fruitList.indexOf(fruit)]
            fruit.declaredFields.mapNotNull {  field ->
                field.isAccessible = true
                val value = getSeedFromField(field, obj) ?: return@mapNotNull null
                Pair(field.name.lowercase(), value)
            }
        }.flatten()
        val seedMethods = fruitList.map { fruit ->
            val obj = objectList[fruitList.indexOf(fruit)]
            fruit.declaredMethods.mapNotNull {  method ->
                val value = getSeedFromMethod(method, seedFields, obj) ?: return@mapNotNull null
                Pair(method.name.lowercase(), value)
            }
        }.flatten()
        return (seedMethods + seedFields).toMap()
    }

    private fun getSeedFromField(field: Field, obj: Any): Any?{
        if(field.annotations.any { it.annotationClass.simpleName == Seed::class.java.simpleName }){
            return field[obj]
        }
        return null
    }

    private fun getSeedFromMethod(method: Method, seeds: List<Any>, obj: Any): Any? {
        if(method.annotations.any { it.annotationClass.simpleName == Seed::class.java.simpleName}){
            val unorderedParameters = seeds.filter { value ->
                method.parameters.any {
                    parameter ->
                        parameter.javaClass.simpleName == value.javaClass.simpleName
                }
            }.associateBy { it.javaClass.simpleName }
            val orderedParameters = method.parameters.map { unorderedParameters[it.javaClass.simpleName] }
            return method.invoke(obj,*orderedParameters.toTypedArray())
        }
        return null
    }
}