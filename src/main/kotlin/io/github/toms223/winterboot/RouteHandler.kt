package io.github.toms223.winterboot

import org.http4k.routing.RoutingHttpHandler
import kotlin.reflect.KClass


class RouteHandler {
    private val methodProcessor = MethodProcessor()
    private val objectInstantiation = ObjectInstantiation()

    fun get(seedsMap: Map<String, Any>, controllerList: List<KClass<out Any>>): List<RoutingHttpHandler> {
        val objControllerList = controllerList.map { objectInstantiation.instantiateObject(it,seedsMap) }
        return controllerList.map { methodProcessor.methodsToRoutes(objControllerList[controllerList.indexOf(it)], it) }.flatten()
    }
}