package com.toms223.kotlinreflection.mappings

@Target(AnnotationTarget.FUNCTION)
annotation class PutMapping(val path: String)
