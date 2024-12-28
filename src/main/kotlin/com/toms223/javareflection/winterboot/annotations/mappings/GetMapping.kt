package com.toms223.winterboot.annotations.mappings

@Target(AnnotationTarget.FUNCTION)
annotation class GetMapping(val path: String)
