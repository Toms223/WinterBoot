package com.toms223.winterboot.annotations.mappings

@Target(AnnotationTarget.FUNCTION)
annotation class PostMapping(val path: String)
