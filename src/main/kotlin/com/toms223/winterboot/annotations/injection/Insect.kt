package com.toms223.winterboot.annotations.injection

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
annotation class Insect(val type: KClass<out Exception>)
