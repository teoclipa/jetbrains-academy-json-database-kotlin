package jsondatabase.client

import jsondatabase.model.Request
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.InetAddress
import java.net.Socket

fun main(args: Array<String>) {
    val address = "127.0.0.1"
    val port = 23456
    val socket = Socket(InetAddress.getByName(address), port)
    println("Client started!")

    val request = if (args.contains("-in")) {
        val filename = args[args.indexOf("-in") + 1]
        val fileContent = File("C:\\Users\\teocl\\Desktop\\intelliJ projects\\JSON Database (Kotlin)\\JSON Database (Kotlin)\\task\\src\\jsondatabase\\client\\data\\$filename").readText()
        Json.decodeFromString<Request>(fileContent)
    } else {
        parseArgs(args)
    }

    if (request == null) {
        println("Invalid arguments")
        return
    }

    val input = DataInputStream(socket.getInputStream())
    val output = DataOutputStream(socket.getOutputStream())

    val requestJson = Json.encodeToString(request)
    output.writeUTF(requestJson)
    println("Sent: $requestJson")

    val responseJson = input.readUTF()
    println("Received: $responseJson")

    socket.close()
}

fun parseArgs(args: Array<String>): Request? {
    var type: String? = null
    var key: JsonElement? = null
    var value: JsonElement? = null

    for (i in args.indices) {
        when (args[i]) {
            "-t" -> type = args.getOrNull(i + 1)
            "-k" -> {
                val keyArg = args.getOrNull(i + 1)
                key = if (keyArg != null && (keyArg.startsWith("[") || keyArg.startsWith("{"))) {
                    Json.parseToJsonElement(keyArg)
                } else {
                    JsonPrimitive(keyArg ?: "")
                }
            }
            "-v" -> {
                val valueArg = args.getOrNull(i + 1)
                value = if (valueArg != null && (valueArg.startsWith("{") || valueArg.startsWith("["))) {
                    Json.parseToJsonElement(valueArg)
                } else {
                    JsonPrimitive(valueArg)
                }
            }
        }
    }

    return if (type != null) {
        Request(type = type, key = key, value = value)
    } else {
        null
    }
}

