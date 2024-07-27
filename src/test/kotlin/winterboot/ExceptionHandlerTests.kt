package winterboot

import ClassFinder
import ExceptionHandler
import SeedFinder
import annotations.injection.Fruit
import annotations.injection.Insect
import annotations.injection.Pesticide
import org.http4k.core.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ExceptionHandlerTests {
    companion object {
        private val classPathUrls = System.getProperty("java.class.path")
                .split(File.pathSeparator)
                .map { File(it).toURI().toURL() }
                .filter { it.path.contains("/test/") }
        private val annotations = listOf(
            Pesticide::class.java,
            Fruit::class.java
        )
        private val classFinder = ClassFinder()
        private val classes = classFinder.findAllClasses(classPathUrls, annotations)
        private val fruits = classes.filter { annotation ->
            annotation.annotations.any { clazz ->
                clazz.annotationClass.simpleName == Fruit::class.java.simpleName
            }
        }
        private val pesticides = classes.filter { annotation ->
            annotation.annotations.any { clazz ->
                clazz.annotationClass.simpleName == Pesticide::class.java.simpleName
            }
        }
        private val seeds = SeedFinder().getSeeds(fruits)
        val exceptionHandler = ExceptionHandler().get(seeds, pesticides)
    }

    @Test
    fun `Should not return throw error`(){
        @Pesticide
        class JustAnotherClass{
            @Insect(IllegalArgumentException::class)
            fun IWontThrow(illegal: IllegalArgumentException): Response {
                return Response(Status.BAD_REQUEST).body(illegal.message?: "IllegalException occurred")
            }
        }

        val request = Request(Method.GET, "/hello")
        assertTrue(exceptionHandler.then{ Response(Status.OK) }(request).status.code == 200)
    }

    @Test
    fun `Should return throw error with one catcher`(){
        @Pesticide
        class JustAnotherClass{
            @Insect(IllegalArgumentException::class)
            fun IllThrow(illegal: IllegalArgumentException): Response {
                return Response(Status.BAD_REQUEST).body(illegal.message?: "IllegalException occurred")
            }
        }

        val request = Request(Method.GET, "/hello")
        assertTrue(exceptionHandler.then{ throw IllegalArgumentException("Im being thrown") }(request).status.code == 400)
    }

    @Test
    fun `Should return throw error with every catcher`(){
        @Pesticide
        class JustAnotherClass{
            @Insect(IllegalArgumentException::class)
            fun WillThrow(illegal: IllegalArgumentException): Response {
                return Response(Status.BAD_REQUEST).body(illegal.message?: "IllegalException occurred")
            }
            @Insect(NullPointerException::class)
            fun MaybeIllThrow(nullPointerException: NullPointerException): Response {
                return Response(Status.INTERNAL_SERVER_ERROR).body(nullPointerException.message?: "NullException occurred")
            }

            @Insect(IndexOutOfBoundsException::class)
            fun IWantToThrow(indexOutOfBoundsException: IndexOutOfBoundsException): Response {
                return Response(Status.BAD_GATEWAY).body(indexOutOfBoundsException.message?: "BadGatewayException occurred")
            }
        }

        val request = Request(Method.GET, "/hello")
        assertTrue(exceptionHandler.then{ throw IllegalArgumentException("Im being thrown") }(request).status.code == 400)
        assertTrue(exceptionHandler.then{ throw NullPointerException("Im being thrown") }(request).status.code == 500)
        assertTrue(exceptionHandler.then{ throw IndexOutOfBoundsException("Im being thrown") }(request).status.code == 502)
    }
}