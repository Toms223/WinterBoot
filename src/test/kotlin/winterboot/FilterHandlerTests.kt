package winterboot

import ClassFinder
import FilterHandler
import SeedFinder
import annotations.injection.Branch
import annotations.injection.Fruit
import annotations.injection.Leaf
import org.http4k.core.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilterHandlerTests {

    companion object {
        private val classPathUrls = System.getProperty("java.class.path")
                .split(File.pathSeparator)
                .map { File(it).toURI().toURL() }
                .filter { it.path.contains("/test/") }
        private val annotations = listOf(
            Branch::class.java,
            Fruit::class.java
        )
        private val classFinder = ClassFinder()
        private val classes = classFinder.findAllClasses(classPathUrls, annotations)
        private val fruits = classes.filter { annotation ->
            annotation.annotations.any { clazz ->
                clazz.annotationClass.simpleName == Fruit::class.java.simpleName
            }
        }
        private val branches = classes.filter { annotation ->
            annotation.annotations.any { clazz ->
                clazz.annotationClass.simpleName == Branch::class.java.simpleName
            }
        }
        private val seeds = SeedFinder().getSeeds(fruits)
        val filterHandler = FilterHandler().get(seeds, branches)
    }

    @Test
    fun `Should return a filter and pass the request`(){
        @Branch
        class YouShallPass{
            @Leaf
            fun iWillNotBlock(next: HttpHandler): HttpHandler {
                return {
                    val request = it.header("modified","true")
                    next(request).header("hello","world")
                }
            }
        }
        val request = Request(Method.GET, "/hello")
        val response = filterHandler.then{
            assertTrue { it.header("modified") == "true" }
            Response(Status.OK)}(request)
        assertEquals(response.status, Status.OK)
        assertTrue(response.header("hello") == "world")
    }

    @Test
    fun `Should return a filter and block the request`(){
        @Branch
        class YouShallNotPass{
            @Leaf
            fun iWillBlock(next: HttpHandler): HttpHandler {
                return {
                    if(it.header("filtered") == "true"){
                        Response(Status.BAD_REQUEST)
                    } else {
                        next(it).header("filtered","false")
                    }
                }
            }
        }
        val request = Request(Method.GET, "/hello")
        val requestModified = request.header("filtered","true")
        val response = filterHandler.then{
            Response(Status.OK)}(request)
        val filteredResponse = filterHandler.then{
            Response(Status.OK)}(requestModified)
        assertEquals(Status.OK, response.status)
        assertTrue(response.header("hello") == "world")
        assertEquals(Status.BAD_REQUEST, filteredResponse.status)
        assertTrue(filteredResponse.header("filtered") != "false")
    }
}