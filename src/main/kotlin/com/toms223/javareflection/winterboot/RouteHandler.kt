package com.toms223.winterboot

import com.toms223.kotlinreflection.ObjectInstantiation
import org.http4k.routing.RoutingHttpHandler


class RouteHandler {
    private val methodProcessor = MethodProcessor()
    private val objectInstantiation = ObjectInstantiation()

    fun get(seedsMap: Map<String, Any>, controllerList: List<Class<*>>): List<RoutingHttpHandler> {
        val objControllerList = controllerList.map { objectInstantiation.instantiateObject(it,seedsMap) }
        return controllerList.map { methodProcessor.methodsToRoutes(objControllerList[controllerList.indexOf(it)], it) }.flatten()
    }
}