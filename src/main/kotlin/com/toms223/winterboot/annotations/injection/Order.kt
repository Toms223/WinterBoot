package com.toms223.winterboot.annotations.injection

@Target(AnnotationTarget.FUNCTION)
annotation class Order(val value: Int = 0)
