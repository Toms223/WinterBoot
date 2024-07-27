package winterboot

import MethodProcessor
import annotations.mappings.DeleteMapping
import annotations.mappings.GetMapping
import annotations.mappings.PostMapping
import annotations.mappings.PutMapping
import annotations.parameters.Cookie
import annotations.parameters.Header
import annotations.parameters.Path
import annotations.parameters.Query
import annotations.parameters.Body
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.http4k.core.*
import org.http4k.core.cookie.cookie
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class MethodProcessorTests {

    companion object {
        class TestController(private val testValue: String){
            init {
                println(testValue)
            }

            companion object {
                @Serializable
                class AnotherClass(val someVal: Int)
            }
            @GetMapping("/testget")
            fun simpleGet(): String{
                return "GetHelloWorld"
            }

            @PutMapping("/testput")
            fun simplePut(): String{
                return "PutHelloWorld"
            }

            @PostMapping("/testpost")
            fun simplePost(): String{
                return "PostHelloWorld"
            }

            @DeleteMapping("/testdelete")
            fun simpleDelete(): String{
                return "DeleteHelloWorld"
            }

            @GetMapping("/test/{path}")
            fun parameterTest(@Path path: Int): Int{
                println(path)
                return path
            }

            @GetMapping("/query")
            fun queryTest(@Query query: Double): Double {
                println(query)
                return query
            }

            @GetMapping("/queries")
            fun queriesTest(@Query query: List<Double>): List<Double> {
                println(query)
                return query
            }

            @GetMapping("/cookies")
            fun cookiesTest(@Cookie cookie: String): String {
                println(cookie)
                return "$cookie Hello World"
            }

            @PutMapping("/body")
            fun bodyTest(@Body someClass: AnotherClass): AnotherClass{
                println(someClass.someVal)
                return someClass
            }

            @GetMapping("header")
            fun headerTest(@Header header: String): String {
                println(header)
                return header
            }
        }
        val testController = TestController("")
        val methodProcessor = MethodProcessor()
    }
    @Test
    fun `Should return simple get route`(){
        val routeList = methodProcessor.methodsToRoutes(testController, testController::class.java)
        assertTrue { routeList.any {
            it.description == ("/testget" bind Method.GET to { Response(Status.OK).body(testController.simpleGet())}).description
        }}
        val app = routes(routeList)
        assertEquals(app(Request(Method.GET,"/testget")).body.toString(),"\"GetHelloWorld\"")
    }

    @Test
    fun `Should return simple put route`(){
        val routeList = methodProcessor.methodsToRoutes(testController, testController::class.java)
        assertTrue { routeList.any {
            it.description == ("/testput" bind Method.PUT to {
                Response(Status.OK).body(testController.simplePut())
            }).description
        }}
        val app = routes(routeList)
        assertEquals(app(Request(Method.PUT,"/testput")).body.toString(),"\"PutHelloWorld\"")
    }

    @Test
    fun `Should return simple post route`(){
        val routeList = methodProcessor.methodsToRoutes(testController, testController::class.java)
        assertTrue { routeList.any {
            it.description == ("/testpost" bind Method.POST to {
                Response(Status.OK).body(testController.simplePost())
            }).description
        }}
        val app = routes(routeList)
        assertEquals(app(Request(Method.POST,"/testpost")).body.toString(),"\"PostHelloWorld\"")
    }

    @Test
    fun `Should return simple delete route`(){
        val routeList = methodProcessor.methodsToRoutes(testController, testController::class.java)
        assertTrue { routeList.any {
            it.description == ("/testdelete" bind Method.DELETE to {
                Response(Status.OK).body(testController.simpleDelete())
            }).description
        }}
        val app = routes(routeList)
        assertEquals(app(Request(Method.DELETE,"/testdelete")).body.toString(),"\"DeleteHelloWorld\"")
    }

    @Test
    fun `Should return get with path parameters`(){
        val routeList = methodProcessor.methodsToRoutes(testController, testController::class.java)
        assertTrue { routeList.any {
            it.description == ("/test/{path}" bind Method.GET to {
                req: Request -> Response(Status.OK).body(
                Json.encodeToString(testController.parameterTest(req.path("path")!!.toInt())))
            }).description
        }}
        val app = routes(routeList)
        assertEquals(app(Request(Method.GET,"/test/123")).body.toString(),"123")
    }

    @Test
    fun `Should return get with query single parameters`(){
        val routeList = methodProcessor.methodsToRoutes(testController, testController::class.java)
        assertTrue { routeList.any {
            it.description == ("/query" bind Method.GET to {
                req: Request -> Response(Status.OK).body(
                Json.encodeToString(testController.queryTest(req.query("query")!!.toDouble()))
                )
            }).description
        }}
        val app = routes(routeList)
        assertEquals(app(Request(Method.GET,"/query").query("query","2.123123")).body.toString(),"2.123123")
    }

    @Test
    fun `Should return get with query multiple parameters`(){
        val routeList = methodProcessor.methodsToRoutes(testController, testController::class.java)
        assertTrue { routeList.any {
            it.description == ("/queries" bind Method.GET to { req: Request -> Response(Status.OK).body(Json.encodeToString(testController.queriesTest(req.query("query")!!.split(',').map { string-> string.toDouble() })))}).description
        }}
        val app = routes(routeList)
        assertEquals(app(Request(Method.GET,"/queries").query("query","2.123123, 1238283.2, 128777.2")).body.toString(),"{\"data\":[2.123123,1238283.2,128777.2]}")
    }

    @Test
    fun `Should return get with body parameters`(){
        val routeList = methodProcessor.methodsToRoutes(testController, testController::class.java)
        assertTrue { routeList.any {
            it.description == ("/body" bind Method.PUT to { req: Request -> Response(Status.OK).body(
                Json.encodeToString(testController.bodyTest(
                    Json.decodeFromString(
                        req.body.toString()
                    )
                ))
            )}).description
        }}
        val app = routes(routeList)
        assertEquals(
            app(Request(Method.PUT,"/body")
                .body(
                    Json.encodeToString(
                        TestController.Companion.AnotherClass(123)
                    )
                )
            ).body.toString(),Json.encodeToString(TestController.Companion.AnotherClass(123)))
    }

    @Test
    fun `Should return get with cookie parameters`(){
        val routeList = methodProcessor.methodsToRoutes(testController, testController::class.java)
        assertTrue { routeList.any {
            it.description == ("/cookies" bind Method.GET to { req: Request -> Response(Status.OK).body(testController.cookiesTest(req.cookie("cookie")!!.value))}).description
        }}
        val app = routes(routeList)
        assertEquals(app(Request(Method.GET,"/cookies").cookie("cookie", "I'm going to say")).body.toString(),"\"I'm going to say Hello World\"")
    }

    @Test
    fun `Should return get with header parameters`(){
        val routeList = methodProcessor.methodsToRoutes(testController, testController::class.java)
        assertTrue { routeList.any {
            it.description == ("/header" bind Method.GET to { req: Request -> Response(Status.OK).body(testController.headerTest(req.header("header")!!))}).description
        }}
        val app = routes(routeList)
        assertEquals(app(Request(Method.GET,"/header").header("header","I'm a header!")).body.toString(),"\"I'm a header!\"")
    }
}