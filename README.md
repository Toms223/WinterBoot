# WinterBoot
An annotation wrapper for ease of creation and handling of routes in HTTP4K based on springboot

#### The "fine i´ll do it myself" version of SpringBoot™

## Motivation
At the start of the LS class it was made quite clear that no external libraries were to be used within the scope of the project.\
**HOWEVER**, no one said anything about creating your own.\
**So here we are.**

## Description
#### What is it?
The WinterBoot (_™ pending_) is a IOC tool to automatically create, filter and handle http server routes.\
It uses Http4k as the server backbone and can be seperated in 4 parts:
* **Routes**
* **Values or "Seeds"**
* **Filters or "Branches"**
* **Exceptions or "Pesticides"**

### Routes
Routes are created by analyzing all classes within the jar that contain the annotation `@Controller` for mappings.\
The current available mappings are `@GetMapping`, `@PutMapping`, `@PostMapping` and `@DeleteMapping` and are used on the class methods.\
All the mapping annotations receive a path value, which can have parameters in them, by using brackets surrounding the name of the path parameter.\
For the function to receive the path parameter it needs to have the same name as the one given in the mapping, and be annotated with `@Path`.\
There are other annotations as well, for the remaining parameters. `@Query`, `@Body` and `@Cookie`.\
Bellow is an example controller that implemented.\
_Note: for body parameters and automatic JSON response, the objects must be serializable using: `kotlinx.serialization`_
#### Example
```kotlin
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
        }
```
Each time a mapping is found it is converted to a http4k route in the following format:
```kotlin
path bind httpMethod to {req -> Response(methodToStatusMap[httpMethod] ?: Status.OK).body(
    method.invoke(obj,*getRequestParameters(req,parameters)).toJsonString()
)}
```
Where path is the annotation stored path, the httpMethod is calculated from the type of annotation and the response is the result of the invoking method serialized to a JSON string with status code associated to the httpMethod used.

#### But hey, how do I dynamically pass `testValue` to my class?
That leads me to my next point:

### Values or Seeds
Many controllers, filters or exception handlers might need constructor parameters of some kind, so we need to inject them when invoking them.\
To do so, we use a class annotated with `@Fruit` and we place our to be injected values in it.\
They can be either fields or functions, as long as they have the same name as the constructor value and are annotated with `@Seed`.
Bellow is an example of a `@Fruit` class:
```kotlin
    @Fruit
    class TestControllerTrunk{
        @Seed
        val testValue = "I was born into this world, hi!"
    }
```
When a Seed is found the field value or the function result is stored in a map with the name of the field/function as the key.  
Winter also makes a distinction between main package and test package seeds. This is so you are able to more easily implement test enviroments which sometimes make use of, for example, in memory data storage instead of database storage or another implementation that is test specific.
### Filters or Branches
Sometimes we need to filter an http request or add some intermediate action before processing one. To do so, we make use of filters or "branches".\
A filter is just a function that takes in the next `HttpHandler` and returns a new one. The new one can then be invoked to return the `Response` of the "next" handler or something completely different.\
Winter makes use of the annotation `@Branch` to denote a class that houses filter functions.\
Those filter functions make use of the `@Leaf` annotation.\
Bellow is an example of a `@Branch` class:
```kotlin
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
```
When a leaf is encountered it is converted to a `Filter` of the Http4k module in the following manner:
```kotlin
    Filter { next ->
        { request ->
            val parameters = method.parameters.mapNotNull  {
                when(it.name.lowercase()) {
                    "next" -> next
                    else -> seedMap[it.name.lowercase()]
                }
            }.toTypedArray()
            (method.invoke(obj,*parameters) as HttpHandler).invoke(request)
        }
    }
```
### Exceptions or Pesticides

At last, we have exceptions, sometimes they occur in our program and we need a way to process them.\
In Winter, exceptions handlers are just really specific filters.
Instead of receiving the next, the exception handling functions are just responsible for receiving the exception, and returning the response.\
We make use of the `@Pesticide` annotation to denote a class that has exception handling functions, and the `@Insect` for the functions.
The function annotation receives the class of the exception to be received by it.
Bellow is an example of a `@Pesticide` class:
```kotlin
@Pesticide
class JustAnotherClass{
    @Insect(IllegalArgumentException::class)
    fun IWontThrow(illegal: IllegalArgumentException): Response {
        return Response(Status.BAD_REQUEST).body(illegal.message?: "IllegalException occurred")
    }
}
```
Whenever it counters an Insect the function is converted to a filter of the following structure:
```kotlin
Filter {
    next -> {
        try{
            next(it)
        } catch (e: Exception){
            val methodException = method.getAnnotation(Insect::class.java).type.java
            if(e.javaClass == methodException){
                constructResponse(method, e, seedMap, obj)
            } else {
                throw e
            }
        }
    }
}
```
In the code snippet, _constructResponse()_ invokes the function the same way a regular filter does.

## Executing

After describing your controllers, filters and exception handlers you can execute the setup for the server with
```kotlin
Winter.setup()
```
Which will return an Http4kServer instance that you can then control however you see fit.  
It is also possible to make use of the Http4k SPA module with the use of the following syntax when setting up the server:
```kotlin
Winter.addSinglePageApplication("your-spa-directory").setup()
```