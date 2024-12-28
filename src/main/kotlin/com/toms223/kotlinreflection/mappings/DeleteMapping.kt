package com.toms223.kotlinreflection.mappings

@Target(AnnotationTarget.FUNCTION)
annotation class DeleteMapping(val path: String)
