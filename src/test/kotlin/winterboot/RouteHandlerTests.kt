package winterboot

import ClassFinder
import RouteHandler
import SeedFinder
import annotations.Controller
import annotations.injection.Fruit
import annotations.injection.Seed
import annotations.mappings.GetMapping
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class RouteHandlerTests {
    class SimpleClass

    companion object {
        @Fruit
        class TestControllerTrunk{
            @Seed
            val testValue = "I was born into this world, hi!"
        }
        private val classFinder = ClassFinder()
        private val annotations = listOf(
            Controller::class.java,
            Fruit::class.java
        )
        private val classPathUrls = System.getProperty("java.class.path")
                .split(File.pathSeparator)
                .map { File(it).toURI().toURL() }
                .filter { it.path.contains("/test/") }
        private val classes = classFinder.findAllClasses(classPathUrls, annotations)
        private val controllers = classes.filter { annotation ->
            annotation.annotations.any { clazz ->
                clazz.annotationClass.simpleName == Controller::class.java.simpleName
            }
        }
        private val fruits = classes.filter { annotation ->
            annotation.annotations.any { clazz ->
                clazz.annotationClass.simpleName == Fruit::class.java.simpleName
            }
        }
        private val seeds = SeedFinder().getSeeds(fruits)
        val routesList = RouteHandler().get(seeds, controllers)
    }
    @Test
    fun `Should create routes from controller without constructor parameters`(){
        @Controller
        class ControllerWithoutConstructorParameters{
            @GetMapping("/simple/path")
            fun getSimplePath(): String {
                return "Hello World"
            }
        }
        assert(routesList.isNotEmpty())
        assert(routesList.any { it.description == ("/simple/path" bind Method.GET to { Response(Status.OK).body(
            ControllerWithoutConstructorParameters().getSimplePath())}).description })
        assertEquals("\"Hello World\"", routes(routesList)(Request(Method.GET,"/simple/path")).bodyString())
    }

    @Test
    fun `Should create routes from controller with constructor parameters from single trunk with seeds`(){
        @Controller
        class ControllerWithConstructorParameters(val first: Int, val second: String, val third: List<Double>){
            @GetMapping("/super/path")
            fun getSimplePath(): String {
                return "Hello World = $first with $second and ${third.map { it.toString() }}"
            }
        }

        @Fruit
        class ControllerTrunk{
            @Seed
            val first = 123

            @Seed
            fun second() = "Im the second"

            @Seed
            val third = listOf(2.1,123.3,16.3)
        }
        assert(routesList.isNotEmpty())
        assert(routesList.any { it.description == ("/super/path" bind Method.GET to { Response(Status.OK).body(
            ControllerWithConstructorParameters(123,"Im the second",listOf(2.1,123.3,16.3)).getSimplePath())}).description })
        assertEquals(
            "\"Hello World = 123 with Im the second and ${listOf(2.1,123.3,16.3).map { it.toString() }}\"",
            routes(routesList)(Request(Method.GET,"/super/path")).bodyString())
    }

    @Test
    fun `Should create routes from controller with constructor parameters from multiple fruit with seeds`(){
        @Controller
        class AnotherControllerWithConstructorParameters(val first: Int, val second: String, val third: List<Double>, val forth: SimpleClass){
            @GetMapping("/ultra/path")
            fun getSimplePath(): String {
                return "Hello World = $first with $second and ${third.map { it.toString() }}"
            }
        }

        @Fruit
        class FirstControllerTrunk{
            @Seed
            val first = 123

            @Seed
            fun second() = "Im the second"

        }

        @Fruit
        class SecondControllerTrunk{
            @Seed
            val forth = SimpleClass()

            @Seed
            val third = listOf(2.1,123.3,16.3)
        }
        assert(routesList.isNotEmpty())
        assert(routesList.any { it.description == ("/super/path" bind Method.GET to { Response(Status.OK).body(
            AnotherControllerWithConstructorParameters(123,"Im the second",listOf(2.1,123.3,16.3),SimpleClass()).getSimplePath())}).description })
        assertEquals(
            "\"Hello World = 123 with Im the second and ${listOf(2.1,123.3,16.3).map { it.toString() }}\"",
            routes(routesList)(Request(Method.GET,"/super/path")).bodyString())
    }

}