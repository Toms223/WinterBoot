package com.toms223.kotlinreflection.mappings

@Target(AnnotationTarget.FUNCTION)
annotation class PostMapping(val path: String)
