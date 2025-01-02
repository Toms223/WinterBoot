package com.toms223.winterboot

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

class ObjectInstantiation {
    fun instantiateObject(kClass: KClass<out Any>, seeds: Map<String, Any>): Any {
        val constructor = getValidConstructor(kClass, seeds)
        val parameters = constructor.parameters.map { seeds[it.name?.lowercase()] }
        return constructor.call(*parameters.toTypedArray()) ?:
        throw NullPointerException("Constructor called but returned null for class: ${kClass.simpleName}")
    }

    private fun getValidConstructor(kClass: KClass<out Any>, seeds: Map<String, Any>): KFunction<*> {
        val primaryConstructor = kClass.primaryConstructor
        if(primaryConstructor != null){
            return primaryConstructor
        }
        val constructors = kClass.constructors
        constructors.forEach { constructor ->
            if(constructor.parameters.all { parameter ->
                seeds.contains(parameter.name?.lowercase())
            }) return constructor
        }
        throw IllegalArgumentException("No seeds found for constructor of class : ${kClass.simpleName}")
    }
}