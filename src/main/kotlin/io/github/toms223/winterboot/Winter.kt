package io.github.toms223.winterboot


import io.github.toms223.winterboot.annotations.injection.Branch
import io.github.toms223.winterboot.annotations.injection.Controller
import io.github.toms223.winterboot.annotations.injection.Fruit
import io.github.toms223.winterboot.annotations.injection.Pesticide
import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.then
import org.http4k.routing.ResourceLoader
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes
import org.http4k.routing.singlePageApp
import java.io.File
import java.time.Instant
import kotlin.reflect.KClass

/**
 * The base class for creating filters, exception handlers and routes from classes
 * annotated with @Branch, @Pesticide and Controller respectively.
 *
 * When using this class to generate your routes make sure there is at least one controller in your
 * source.
 *
 * @param singlePageApplication an SPA parameter from HTTP4K
 */
class Winter(private val singlePageApplication: RoutingHttpHandler) {
    companion object {
        private val routeHandler = RouteHandler()
        private val filterHandler = FilterHandler()
        private val exceptionHandler = ExceptionHandler()
        private val seeds: Map<String, Any>
        private val controllers: List<KClass<out Any>>
        private val branches: List<KClass<out Any>>
        private val pesticides: List<KClass<out Any>>
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
                Controller::class,
                Branch::class,
                Pesticide::class,
                Fruit::class
            )
            val mainClasses = classFinder.findAllClasses(classPathUrls.filter { it.path.contains("/main/") }, annotations).ifEmpty {
                classFinder.findAllClasses(classPathUrls, annotations)
            }
            val testClasses = classFinder.findAllClasses(classPathUrls.filter { it.path.contains("/test/") }, annotations)
            val fruitClasses = testClasses.ifEmpty { mainClasses }
            val classes = mainClasses + testClasses
            println("Took ${Instant.now().toEpochMilli() - classFindingTime.toEpochMilli()} milliseconds to find Classes")
            val fruits = fruitClasses.filter { kClass ->
                kClass.annotations.any { annotation ->
                    annotation.annotationClass == Fruit::class
                }
            }
            branches = classes.filter { kClass ->
                kClass.annotations.any { annotation ->
                    annotation.annotationClass == Branch::class
                }
            }
            pesticides = classes.filter { kClass ->
                kClass.annotations.any { annotation ->
                    annotation.annotationClass == Pesticide::class
                }
            }
            controllers = classes.filter { kClass ->
                kClass.annotations.any { annotation ->
                    annotation.annotationClass == Controller::class
                }
            }
            val seedFindingTime = Instant.now()
            seeds = io.github.toms223.winterboot.SeedFinder().getSeeds(fruits)
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

        /**
         * Base function to create RoutingHttpHandler
         * @return RoutingHttpHandler
         */
        fun setup(): RoutingHttpHandler {
            return exceptions.then(filters).then(routes(routeList))
        }
    }

    /**
     * Base function to create RoutingHttpHandler to be fed into HTTP4K server
     * Can only be used in case SPA is specified
     * @return RoutingHttpHandler
     */
    fun setup(): RoutingHttpHandler {
        return exceptions.then(filters).then(routes(routeList + singlePageApplication))

    }
}