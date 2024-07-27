package processors

import annotations.parameters.*
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
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import java.time.Instant
import org.http4k.core.Method as HttpMethod

class Parameterized {

    companion object {
        private val methodToStatusMap = mapOf(
            HttpMethod.POST to Status.CREATED
        )

        fun process(
            method: Method, obj: Any, mapEntry: Map.Entry<Class<out Annotation>, HttpMethod>, path: String)
        : RoutingHttpHandler {
            val parameters = getParameters(method)
            val httpMethod = mapEntry.value
            return path.lowercase() bind httpMethod to {req -> Response(methodToStatusMap[httpMethod] ?: Status.OK)
                .body(method.invoke(obj,*getRequestParameters(req,parameters))?.toJsonString() ?: "")
                .header("Content-Type", "application/json")
            }
        }
        private fun getRequestParameters(req: Request, parameters: List<Parameter>): Array<Any?>{
            return parameters.map { parameter ->
                    processParameterType(parameter.annotations.first(),req,parameter.name,parameter)
            }.toTypedArray()
        }

        private fun processParameterType(annotation: Annotation, req: Request, name: String, parameter: Parameter): Any?{
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
                    if(List::class.java.isAssignableFrom(parameter.type)){
                        if(req.query(name) == null) throw IllegalArgumentException("No query argument found.")
                        val listType = parameter.parameterizedType as ParameterizedType
                        return req.query(name)!!.split(',').map { typeConverter(it, listType.actualTypeArguments[0] as Class<*>) }
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

        private fun deserializeBody(jsonString: String, type: Class<*>): Any{
            val json = Json { ignoreUnknownKeys = true }
            val jsonElement = json.parseToJsonElement(jsonString)
            val serializer = json.serializersModule.serializer(type)
            return json.decodeFromJsonElement(serializer, jsonElement)
        }

        private fun <T> typeConverter(value: String, valueType: Class<T>): Any {
            return when (valueType.simpleName.lowercase()) {
                "integer" -> value.toInt()
                "int" -> value.toInt()
                "float" -> value.toFloat()
                "double" -> value.toDouble()
                "bool" -> value.toBoolean()
                "short" -> value.toShort()
                "long" -> value.toLong()
                "instant" -> Instant.parse(value)
                else -> value
            }
        }

        private fun getParameters(method: Method): List<Parameter>{
            return method.parameters.filter {
                it.isAnnotationPresent(Cookie::class.java) ||
                        it.isAnnotationPresent(Path::class.java) ||
                        it.isAnnotationPresent(Query::class.java) ||
                        it.isAnnotationPresent(Body::class.java) ||
                        it.isAnnotationPresent(Header::class.java)
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