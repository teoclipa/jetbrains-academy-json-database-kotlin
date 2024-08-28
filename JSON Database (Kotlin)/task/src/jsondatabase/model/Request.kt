package jsondatabase.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Request(
    val type: String,
    val key: JsonElement? = null,
    val value: JsonElement? = null
)
