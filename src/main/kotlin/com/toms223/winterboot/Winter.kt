package com.toms223.winterboot


import com.toms223.winterboot.annotations.Controller
import com.toms223.winterboot.annotations.injection.Branch
import com.toms223.winterboot.annotations.injection.Fruit
import com.toms223.winterboot.annotations.injection.Pesticide
import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.then
import org.http4k.routing.ResourceLoader
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes
import org.http4k.routing.singlePageApp
import java.io.File
import java.net.URLDecoder
import java.time.Instant

class Winter(private val singlePageApplication: RoutingHttpHandler? = null) {
    companion object {
        private val routeHandler = RouteHandler()
        private val filterHandler = FilterHandler()
        private val exceptionHandler = ExceptionHandler()
        private val seeds: Map<String, Any>
        private val controllers: List<Class<*>>
        private val branches: List<Class<*>>
        private val pesticides: List<Class<*>>
        private val routeList: List<RoutingHttpHandler>
        private val filters: Filter
        private val exceptions: Filter
        init {
            println(" __      __.__        __                \n" +
                    "/  \\    /  \\__| _____/  |_  ___________ \n" +
                    "\\   \\/\\/   /  |/    \\   __\\/ __ \\_  __ \\\n" +
                    " \\        /|  |   |  \\  | \\  ___/|  | \\/\n" +
                    "  \\__/\\  / |__|___|  /__|  \\___  >__|   \n" +
                    "       \\/          \\/          \\/       ")
            val classFindingTime = Instant.now()
            val classFinder = ClassFinder()
            val classPathUrls = System.getProperty("java.class.path")
                .split(File.pathSeparator)
                .map { File(it).toURI().toURL() }
            val annotations = listOf(
                Controller::class.java,
                Branch::class.java,
                Pesticide::class.java,
                Fruit::class.java
            )
            val mainClasses = classFinder.findAllClasses(classPathUrls.filter { it.path.contains("/main/") }, annotations)
            val testClasses = classFinder.findAllClasses(classPathUrls.filter { it.path.contains("/test/") }, annotations)
            val fruitClasses = testClasses.ifEmpty { mainClasses }
            val classes = mainClasses + testClasses
            println("Took ${Instant.now().toEpochMilli()- classFindingTime.toEpochMilli()} milliseconds to find Classes")
            val fruits = fruitClasses.filter { annotation ->
                annotation.annotations.any { clazz ->
                    clazz.annotationClass.simpleName == Fruit::class.java.simpleName
                }
            }
            branches = classes.filter { annotation ->
                annotation.annotations.any { clazz ->
                    clazz.annotationClass.simpleName == Branch::class.java.simpleName
                }
            }
            pesticides = classes.filter { annotation ->
                annotation.annotations.any { clazz ->
                    clazz.annotationClass.simpleName == Pesticide::class.java.simpleName
                }
            }
            controllers = classes.filter { annotation ->
                annotation.annotations.any { clazz ->
                    clazz.annotationClass.simpleName == Controller::class.java.simpleName
                }
            }
            val seedFindingTime = Instant.now()
            seeds = SeedFinder().getSeeds(fruits)
            println("Took ${Instant.now().toEpochMilli() - seedFindingTime.toEpochMilli()} milliseconds to find Seeds")
            val exceptionProcessingTime = Instant.now()
            exceptions = exceptionHandler.get(seeds, pesticides)
            println("Took ${Instant.now().toEpochMilli() - exceptionProcessingTime.toEpochMilli()} milliseconds to process Exception Handlers")
            val filterProcessingTime = Instant.now()
            filters = filterHandler.get(seeds, branches)
            println("Took ${Instant.now().toEpochMilli() - filterProcessingTime.toEpochMilli()} milliseconds to process Filter Handlers")
            val routeProcessingTime = Instant.now()
            routeList = routeHandler.get(seeds, controllers)
            println("Took ${Instant.now().toEpochMilli() - routeProcessingTime.toEpochMilli()} milliseconds to process Route Handlers")
            println("The Winter is coming!")
        }

        fun addSinglePageApplication(dir: String): Winter{
            val spa = singlePageApp(
                ResourceLoader.Directory(dir),
                ".js" to ContentType.APPLICATION_JSON,
                ".html" to ContentType.TEXT_HTML,
                ".css" to ContentType.Text("text/css")
            )
            return Winter(spa)
        }

        fun setup(): RoutingHttpHandler {
            return exceptions.then(filters).then(routes(routeList))
        }
    }

    fun setup(): RoutingHttpHandler {
        return if (singlePageApplication != null) {
            exceptions.then(filters).then(routes(routeList + singlePageApplication))
        } else {
            exceptions.then(filters).then(routes(routeList))
        }
    }
}