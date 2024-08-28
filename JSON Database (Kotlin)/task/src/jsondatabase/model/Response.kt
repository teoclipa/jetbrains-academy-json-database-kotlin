package jsondatabase.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Response(
    val response: String,
    val value: JsonElement? = null,
    val reason: String? = null
)
