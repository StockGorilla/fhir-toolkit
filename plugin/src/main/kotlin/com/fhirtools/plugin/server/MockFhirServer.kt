package com.fhirtools.plugin.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Lightweight in-memory mock FHIR server. Echoes back what's POSTed; no validation,
 * no FHIR search params (just list-all), no auth. For developer testing of FHIR
 * client wiring without standing up a full HAPI server.
 *
 * NOT a production-grade server. Intentionally minimal — adding Jetty + a real
 * HAPI RestfulServer would bloat the plugin JAR by 5–10 MB for a feature whose
 * value is "is my client wired up correctly?"
 */
class MockFhirServer {

    @Volatile
    private var server: HttpServer? = null

    @Volatile
    private var executor: ExecutorService? = null

    private val storage = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()

    val port: Int
        get() = server?.address?.port ?: -1

    val baseUrl: String
        get() = "http://localhost:$port/fhir"

    val isRunning: Boolean
        get() = server != null

    @Synchronized
    fun start(): String {
        if (server != null) return baseUrl
        val srv = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        srv.createContext("/fhir", FhirHandler(storage) { baseUrl })
        val exec = Executors.newFixedThreadPool(4) { runnable ->
            Thread(runnable, "FhirToolkit-MockServer").apply { isDaemon = true }
        }
        srv.executor = exec
        srv.start()
        server = srv
        executor = exec
        return baseUrl
    }

    @Synchronized
    fun stop() {
        server?.stop(0)
        executor?.shutdown()
        server = null
        executor = null
        storage.clear()
    }
}

private class FhirHandler(
    private val storage: ConcurrentHashMap<String, ConcurrentHashMap<String, String>>,
    private val baseUrlSupplier: () -> String,
) : HttpHandler {

    override fun handle(exchange: HttpExchange) {
        try {
            val path = exchange.requestURI.path.removePrefix("/fhir").removePrefix("/")
            val method = exchange.requestMethod

            when {
                path == "metadata" && method == "GET" -> sendCapabilityStatement(exchange)
                path.isEmpty() -> sendOperationOutcome(
                    exchange, 404, "not-found",
                    "Use /fhir/{ResourceType}/{id} or /fhir/metadata",
                )
                else -> handleResource(exchange, path, method)
            }
        } catch (t: Throwable) {
            sendOperationOutcome(exchange, 500, "exception", t.message ?: t.javaClass.simpleName)
        } finally {
            exchange.close()
        }
    }

    private fun handleResource(exchange: HttpExchange, path: String, method: String) {
        val parts = path.split("/")
        val resourceType = parts[0]
        val id = parts.getOrNull(1)

        when {
            id == null && method == "GET" -> listAll(exchange, resourceType)
            id == null && method == "POST" -> createResource(exchange, resourceType)
            id != null && method == "GET" -> readResource(exchange, resourceType, id)
            id != null && method == "PUT" -> updateResource(exchange, resourceType, id)
            id != null && method == "DELETE" -> deleteResource(exchange, resourceType, id)
            else -> sendOperationOutcome(
                exchange, 405, "not-supported",
                "Method not allowed: $method $path",
            )
        }
    }

    private fun readResource(exchange: HttpExchange, resourceType: String, id: String) {
        val json = storage[resourceType]?.get(id)
        if (json == null) {
            sendOperationOutcome(exchange, 404, "not-found", "Resource $resourceType/$id not found")
        } else {
            sendJson(exchange, 200, json)
        }
    }

    private fun createResource(exchange: HttpExchange, resourceType: String) {
        val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
        if (body.isBlank()) {
            sendOperationOutcome(exchange, 400, "invalid", "Empty request body")
            return
        }
        val id = extractId(body) ?: UUID.randomUUID().toString()
        val resourceWithId = ensureId(body, id)
        storage.computeIfAbsent(resourceType) { ConcurrentHashMap() }[id] = resourceWithId

        exchange.responseHeaders["Location"] = listOf("${baseUrlSupplier()}/$resourceType/$id")
        sendJson(exchange, 201, resourceWithId)
    }

    private fun updateResource(exchange: HttpExchange, resourceType: String, id: String) {
        val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
        if (body.isBlank()) {
            sendOperationOutcome(exchange, 400, "invalid", "Empty request body")
            return
        }
        val resourceWithId = ensureId(body, id)
        val existed = storage[resourceType]?.containsKey(id) == true
        storage.computeIfAbsent(resourceType) { ConcurrentHashMap() }[id] = resourceWithId
        sendJson(exchange, if (existed) 200 else 201, resourceWithId)
    }

    private fun deleteResource(exchange: HttpExchange, resourceType: String, id: String) {
        val existed = storage[resourceType]?.remove(id) != null
        if (existed) {
            exchange.sendResponseHeaders(204, -1)
        } else {
            sendOperationOutcome(exchange, 404, "not-found", "Resource $resourceType/$id not found")
        }
    }

    private fun listAll(exchange: HttpExchange, resourceType: String) {
        val resources = storage[resourceType]?.values?.toList().orEmpty()
        val bundle = buildString {
            append("""{"resourceType":"Bundle","type":"searchset","total":${resources.size}""")
            if (resources.isNotEmpty()) {
                append(""","entry":[""")
                resources.forEachIndexed { i, json ->
                    if (i > 0) append(",")
                    append("""{"resource":""")
                    append(json)
                    append("}")
                }
                append("]")
            }
            append("}")
        }
        sendJson(exchange, 200, bundle)
    }

    private fun sendCapabilityStatement(exchange: HttpExchange) {
        val cs = """{"resourceType":"CapabilityStatement","status":"active","kind":"instance",""" +
            """"fhirVersion":"4.0.1","format":["application/fhir+json","application/json"],""" +
            """"rest":[{"mode":"server"}]}"""
        sendJson(exchange, 200, cs)
    }

    private fun sendOperationOutcome(exchange: HttpExchange, status: Int, code: String, message: String) {
        val safe = message.replace("\\", "\\\\").replace("\"", "\\\"")
        val oo = """{"resourceType":"OperationOutcome","issue":[{"severity":"error","code":"$code","diagnostics":"$safe"}]}"""
        sendJson(exchange, status, oo)
    }

    private fun sendJson(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders["Content-Type"] = listOf("application/fhir+json; charset=utf-8")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }

    private fun extractId(json: String): String? {
        // Adequate for echo-mode: find a top-level "id" string field. Robust enough for
        // FHIR JSON in practice; would need a real parser to handle nested edge cases.
        val pattern = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"")
        return pattern.find(json)?.groupValues?.getOrNull(1)
    }

    private fun ensureId(json: String, id: String): String {
        if (extractId(json) != null) return json
        val rtMatch = Regex("\"resourceType\"\\s*:\\s*\"[^\"]+\"").find(json) ?: return json
        return json.replaceFirst(rtMatch.value, "${rtMatch.value},\"id\":\"$id\"")
    }
}
