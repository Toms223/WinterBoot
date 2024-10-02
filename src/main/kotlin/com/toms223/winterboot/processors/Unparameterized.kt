package com.toms223.winterboot.processors

import com.toms223.winterboot.CustomResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.cookie
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import java.lang.reflect.Method

class Unparameterized {
    companion object {
        private val methodToStatusMap = mapOf(
            org.http4k.core.Method.POST to Status.CREATED
        )
        fun process(
            method: Method, obj: Any, mapEntry: Map.Entry<Class<out Annotation>, org.http4k.core.Method>, path: String)
        : RoutingHttpHandler {
            val httpMethod = mapEntry.value
            return path bind httpMethod to {
                val response = Response(methodToStatusMap[httpMethod] ?: Status.OK)
                val returnValue = method.invoke(obj)
                if (returnValue != null && returnValue.javaClass.isAssignableFrom(CustomResponse::class.java)) {
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