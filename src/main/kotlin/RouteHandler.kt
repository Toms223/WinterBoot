import org.http4k.routing.RoutingHttpHandler


class RouteHandler {
    private val methodProcessor = MethodProcessor()
    private val objectInstantiator = ObjectInstantiator()

    fun get(seedsMap: Map<String, Any>, controllerList: List<Class<*>>): List<RoutingHttpHandler> {
        val objControllerList = controllerList.map { objectInstantiator.instantiateObject(it,seedsMap) }
        return controllerList.map { methodProcessor.methodsToRoutes(objControllerList[controllerList.indexOf(it)], it) }.flatten()
    }
}