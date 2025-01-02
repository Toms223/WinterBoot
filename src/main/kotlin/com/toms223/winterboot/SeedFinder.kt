package com.toms223.winterboot


import com.toms223.winterboot.annotations.injection.Seed
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

class SeedFinder {
    fun getSeeds(fruitList: List<KClass<*>>): Map<String, Any>{
        val objectList = fruitList.map { it.createInstance() }
        val seedFields = fruitList.map { fruit ->
            val obj = objectList[fruitList.indexOf(fruit)]
            fruit.memberProperties.mapNotNull{ property ->
                if(property.annotations.any { it.annotationClass == Seed::class }){
                    val value = (property as KProperty1<Any, *>).get(obj) ?: return@mapNotNull null
                    Pair(property.name.lowercase(), value)
                } else {
                    null
                }

            }
        }.flatten()
        val seedMethods = fruitList.map { fruit ->
            val obj = objectList[fruitList.indexOf(fruit)]
            fruit.memberFunctions.mapNotNull{ function ->
                if(function.annotations.any { it.annotationClass == Seed::class }){
                    val value = function.call(obj) ?: return@mapNotNull null
                    Pair(function.name.lowercase(), value)
                } else {
                    null
                }

            }
        }.flatten()
        return (seedMethods + seedFields).toMap()
    }
}