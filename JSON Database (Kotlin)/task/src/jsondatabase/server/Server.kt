package jsondatabase.server

import jsondatabase.model.Request
import jsondatabase.model.Response
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantReadWriteLock

val lock = ReentrantReadWriteLock()
val readLock = lock.readLock()
val writeLock = lock.writeLock()

fun main() {
    val address = "127.0.0.1"
    val port = 23456
    val server = ServerSocket(port, 50, InetAddress.getByName(address))
    val databaseFile = File("server/data/db.json")

    // Initialize the database file if it doesn't exist
    if (!databaseFile.exists()) {
        databaseFile.parentFile.mkdirs()
        databaseFile.writeText("{}")
    }
    println("Server started!")
    val executor = Executors.newCachedThreadPool()

    while (true) {
        val socket = server.accept()
        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())

        val requestJson = input.readUTF()
        val request = Json.decodeFromString<Request>(requestJson)
        val response = executor.submit<Response> { handleRequest(request, databaseFile) }.get()
        val responseJson = Json.encodeToString(response)
        output.writeUTF(responseJson)
        println("Received: $requestJson")
        println("Sent: $responseJson")

        socket.close()

        if (request.type == "exit") {
            break
        }
    }

    server.close()
}

fun handleRequest(request: Request, databaseFile: File): Response {
    return when (request.type) {
        "get" -> {
            readLock.lock()
            try {
                val database = loadDatabase(databaseFile)
                val keyList = request.key?.let { convertKeyToList(it) }
                    ?: return Response(response = "ERROR", reason = "Invalid key format")
                val value = getValueFromJson(database, keyList)
                if (value != null) {
                    Response(response = "OK", value = value)
                } else {
                    Response(response = "ERROR", reason = "No such key")
                }
            } finally {
                readLock.unlock()
            }
        }

        "set" -> {
            writeLock.lock()
            try {
                if (request.key != null && request.value != null) {
                    val database = loadDatabase(databaseFile).toMutableMap()
                    val keyList = convertKeyToList(request.key)
                        ?: return Response(response = "ERROR", reason = "Invalid key format")
                    setValueInJson(database, keyList, request.value)
                    saveDatabase(databaseFile, JsonObject(database))
                    Response(response = "OK")
                } else {
                    Response(response = "ERROR", reason = "Invalid request")
                }
            } finally {
                writeLock.unlock()
            }
        }

        "delete" -> {
            writeLock.lock()
            try {
                if (request.key != null) {
                    val database = loadDatabase(databaseFile).toMutableMap()
                    val keyList = convertKeyToList(request.key)
                        ?: return Response(response = "ERROR", reason = "Invalid key format")
                    val success = deleteValue(database, keyList)
                    if (success) {
                        saveDatabase(databaseFile, JsonObject(database))
                        Response(response = "OK")
                    } else {
                        Response(response = "ERROR", reason = "No such key")
                    }
                } else {
                    Response(response = "ERROR", reason = "Invalid request")
                }
            } finally {
                writeLock.unlock()
            }
        }

        "exit" -> Response(response = "OK")
        else -> Response(response = "ERROR", reason = "Unknown request type")
    }
}

fun loadDatabase(file: File): JsonObject {
    val content = file.readText()
    return if (content.isNotEmpty()) {
        Json.parseToJsonElement(content).jsonObject
    } else {
        JsonObject(emptyMap())
    }
}

fun saveDatabase(file: File, database: JsonObject) {
    file.writeText(Json.encodeToString(database))
}

fun getValueFromJson(json: JsonObject, path: List<String>): JsonElement? {
    var current: JsonElement = json
    for (key in path) {
        current = (current as? JsonObject)?.get(key) ?: return null
    }
    return current
}

fun setValueInJson(json: MutableMap<String, JsonElement>, path: List<String>, value: JsonElement) {
    var current: MutableMap<String, JsonElement> = json

    for (i in 0 until path.size - 1) {
        val key = path[i]
        val next = current[key]

        if (next is JsonObject) {
            // Update current to point to the mutable map of the next level
            val nextMap = next.toMutableMap()
            current[key] = JsonObject(nextMap)
            current = nextMap
        } else if (next == null) {
            // Create a new JsonObject if the next key doesn't exist
            val newMap = mutableMapOf<String, JsonElement>()
            current[key] = JsonObject(newMap)
            current = newMap
        } else {
            // If we encounter a non-object element where an object is expected, we should replace it
            val newMap = mutableMapOf<String, JsonElement>()
            current[key] = JsonObject(newMap)
            current = newMap
        }
    }

    // Finally, update the value at the last key in the path
    current[path.last()] = value
}

fun deleteValue(db: MutableMap<String, JsonElement>, keys: List<String>): Boolean {
    if (keys.size == 1) {
        return db.remove(keys.first()) != null
    } else {
        val nextKey = keys.first()
        val remainingKeys = keys.drop(1)

        val nextElement = db[nextKey]
        if (nextElement is JsonObject) {
            val nextMap = nextElement.toMutableMap()
            val success = deleteValue(nextMap, remainingKeys)
            if (success) {
                db[nextKey] = JsonObject(nextMap)
            }
            return success
        } else {
            return false
        }
    }
}



fun convertKeyToList(key: JsonElement): List<String>? {
    return if (key is JsonArray) {
        key.map { it.jsonPrimitive.content }
    } else if (key is JsonPrimitive && key.isString) {
        listOf(key.content)
    } else {
        null
    }
}
