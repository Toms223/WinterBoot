package com.toms223.winterboot.processors

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import java.lang.reflect.Method

class Unparameterized {
    companion object {
        fun process(
            method: Method, obj: Any, mapEntry: Map.Entry<Class<out Annotation>, org.http4k.core.Method>, path: String)
        : RoutingHttpHandler {
            val httpMethod = mapEntry.value
            return path bind httpMethod to { Response(Status.OK).body(method.invoke(obj)?.toJsonString() ?: "")
                .header("Content-Type", "application/json") }
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