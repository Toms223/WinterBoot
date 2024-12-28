package com.toms223.kotlinreflection.mappings

@Target(AnnotationTarget.FUNCTION)
annotation class GetMapping(val path: String)
