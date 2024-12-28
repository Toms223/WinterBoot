package com.toms223.javareflection.winterboot.annotations.mappings

@Target(AnnotationTarget.FUNCTION)
annotation class DeleteMapping(val path: String)
