package com.toms223.winterboot.processors

import com.toms223.winterboot.CustomResponse
import com.toms223.winterboot.annotations.parameters.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.cookie
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import org.http4k.core.Method as HttpMethod

class Parameterized {

    companion object {
        private val methodToStatusMap = mapOf(
            HttpMethod.POST to Status.CREATED
        )

        fun process(
            method: KFunction<*>,
            parameters: List<KParameter>,
            obj: Any, mapEntry: Map.Entry<KClass<out Annotation>, HttpMethod>,
            path: String
        ): RoutingHttpHandler {
            val filteredParameters = parameters.filter {
                it.annotations.any {
                    annotation ->
                    annotation.annotationClass == Path::class
                            || annotation.annotationClass == Cookie::class
                            || annotation.annotationClass == Query::class
                            || annotation.annotationClass == Body::class
                            || annotation.annotationClass == Header::class
                }
            }
            val httpMethod = mapEntry.value
            return path.lowercase() bind httpMethod to { req ->
                val response = Response(methodToStatusMap[httpMethod] ?: Status.OK)
                val returnValue = method.call(obj,*getRequestParameters(req,filteredParameters))
                if(returnValue != null && returnValue.javaClass.isAssignableFrom(CustomResponse::class.java)) {
                    val customResponse = returnValue as CustomResponse
                    val cookiedResponse = customResponse.cookies.fold(response) { acc, cookie ->
                        acc.cookie(cookie)
                    }
                    cookiedResponse.body(customResponse.body.toJsonString())
                        .headers(customResponse.headers)
                } else {
                    response.body(returnValue?.toJsonString() ?: "").header("Content-Type", "application/json")
                }

            }
        }
        private fun getRequestParameters(req: Request, parameters: List<KParameter>): Array<Any?>{
            val params = parameters.map { parameter ->
                    processParameterType(parameter.annotations.first(), req, parameter.name!!, parameter)
            }.toTypedArray()
            return params
        }

        private fun processParameterType(annotation: Annotation, req: Request, name: String, parameter: KParameter): Any?{
            return when(annotation){
                is Path -> {
                    typeConverter(req.path(name.lowercase())
                        ?: throw IllegalArgumentException("No path argument found."),
                        parameter.type
                    )
                }
                is Cookie -> {
                    typeConverter(req.cookie(name)?.value
                        ?: throw IllegalArgumentException("No cookie argument found."),
                        parameter.type
                    )
                }
                is Query -> {
                    if(parameter.type.classifier == List::class) {
                        if(req.query(name) == null) throw IllegalArgumentException("No query argument found.")
                        val listType = parameter.type.arguments.first().type!!
                        return req.query(name)!!.split(',').map { typeConverter(it, listType) }
                    }
                    val value = req.query(name) ?: return null
                    typeConverter(value, parameter.type)
                }
                is Body -> {
                    deserializeBody(req.bodyString(),parameter.type)
                }
                is Header -> {
                    typeConverter(req.header(name)
                        ?: throw IllegalArgumentException("No path argument found."),
                        parameter.type
                    )
                }
                else -> null
            }
        }

        private fun deserializeBody(jsonString: String, type: KType): Any? {
            val json = Json { ignoreUnknownKeys = true }
            val jsonElement = json.parseToJsonElement(jsonString)
            val serializer = json.serializersModule.serializer(type)
            return json.decodeFromJsonElement(serializer, jsonElement)
        }

        private fun typeConverter(value: String, valueType: KType): Any {
            return when (valueType) {
                Int::class.createType() -> value.toInt()
                Float::class.createType() -> value.toFloat()
                Double::class.createType() -> value.toDouble()
                Boolean::class.createType() -> value.toBoolean()
                Short::class.createType() -> value.toShort()
                Long::class.createType() -> value.toLong()
                Instant::class.createType() -> Instant.parse(value)
                LocalDate::class.createType() -> LocalDate.parse(value)
                else -> value
            }
        }

        private fun Any?.toJsonElement(): JsonElement = when (this) {
            null -> JsonNull
            is Pair<*, *> -> JsonObject(mapOf("first" to this.first.toJsonElement(), "second" to this.second.toJsonElement()))
            is JsonElement -> this
            is Number -> JsonPrimitive(this)
            is Boolean -> JsonPrimitive(this)
            is String -> JsonPrimitive(this)
            is Array<*> -> JsonObject(mapOf("data" to JsonArray(map { it.toJsonElement() })))
            is List<*> -> JsonObject(mapOf("data" to JsonArray(map { it.toJsonElement() })))
            is Map<*, *> -> JsonObject(map { it.key.toString() to it.value.toJsonElement() }.toMap())
            else -> Json.encodeToJsonElement(serializer(this::class.java), this)
        }

        private fun Any?.toJsonString(): String = Json.encodeToString(this.toJsonElement())
    }
}