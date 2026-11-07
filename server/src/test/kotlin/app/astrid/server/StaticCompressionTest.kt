package app.astrid.server

import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StaticCompressionTest {
    @Test
    fun `large javascript and css use gzip without changing their decoded representation`() = withFixture { fixture ->
        testApplication {
            application { astridModule(config(fixture.root.path), InMemoryAstridRepository()) }

            val script = client.get("/assets/app.js") { encodedWith("gzip") }
            assertEquals(HttpStatusCode.OK, script.status)
            assertEquals("gzip", script.headers[HttpHeaders.ContentEncoding])
            assertVariesByAcceptEncoding(script)
            assertJavaScript(script.contentType())
            assertEquals("no-cache", script.headers[HttpHeaders.CacheControl])
            assertEquals("nosniff", script.headers["X-Content-Type-Options"])
            val compressedScript = script.bodyAsBytes()
            assertTrue(compressedScript.size < fixture.script.size)
            assertContentEquals(fixture.script, compressedScript.gunzip())

            val style = client.get("/assets/app.css") { encodedWith("gzip") }
            assertEquals("gzip", style.headers[HttpHeaders.ContentEncoding])
            assertVariesByAcceptEncoding(style)
            assertEquals(ContentType.Text.CSS, style.contentType()?.withoutParameters())
            assertContentEquals(fixture.style, style.bodyAsBytes().gunzip())

            val identity = client.get("/assets/app.js") { trustedHost() }
            assertNull(identity.headers[HttpHeaders.ContentEncoding])
            assertVariesByAcceptEncoding(identity)
            assertContentEquals(fixture.script, identity.bodyAsBytes())
        }
    }

    @Test
    fun `quality negotiation and the size threshold retain identity when appropriate`() = withFixture { fixture ->
        testApplication {
            application { astridModule(config(fixture.root.path), InMemoryAstridRepository()) }

            val identity = client.get("/assets/app.js") { encodedWith("gzip;q=0.5, identity;q=1") }
            assertNull(identity.headers[HttpHeaders.ContentEncoding])
            assertVariesByAcceptEncoding(identity)
            assertContentEquals(fixture.script, identity.bodyAsBytes())

            val gzip = client.get("/assets/app.js") { encodedWith("identity;q=0.4, gzip;q=0.9") }
            assertEquals("gzip", gzip.headers[HttpHeaders.ContentEncoding])
            assertContentEquals(fixture.script, gzip.bodyAsBytes().gunzip())

            val implicitIdentity = client.get("/assets/app.js") { encodedWith("gzip;q=0.8") }
            assertEquals("gzip", implicitIdentity.headers[HttpHeaders.ContentEncoding])
            assertContentEquals(fixture.script, implicitIdentity.bodyAsBytes().gunzip())

            val forbiddenGzip = client.get("/assets/app.js") { encodedWith("gzip;q=0, identity;q=1") }
            assertNull(forbiddenGzip.headers[HttpHeaders.ContentEncoding])
            assertVariesByAcceptEncoding(forbiddenGzip)
            assertContentEquals(fixture.script, forbiddenGzip.bodyAsBytes())

            val tiny = client.get("/assets/tiny.js") { encodedWith("gzip") }
            assertNull(tiny.headers[HttpHeaders.ContentEncoding])
            assertNull(tiny.headers[HttpHeaders.Vary])
            assertContentEquals(fixture.tinyScript, tiny.bodyAsBytes())
        }
    }

    @Test
    fun `html fonts images api responses and errors are never compressed`() = withFixture { fixture ->
        testApplication {
            application { astridModule(config(fixture.root.path), InMemoryAstridRepository()) }

            val index = client.get("/") { encodedWith("gzip") }
            assertEquals(ContentType.Text.Html, index.contentType()?.withoutParameters())
            assertNull(index.headers[HttpHeaders.ContentEncoding])
            assertNull(index.headers[HttpHeaders.Vary])
            assertEquals("no-cache", index.headers[HttpHeaders.CacheControl])
            assertContentEquals(fixture.index, index.bodyAsBytes())

            val font = client.get("/assets/inter.woff2") { encodedWith("gzip") }
            assertNull(font.headers[HttpHeaders.ContentEncoding])
            assertNull(font.headers[HttpHeaders.Vary])
            assertContentEquals(fixture.font, font.bodyAsBytes())

            val image = client.get("/assets/poster.png") { encodedWith("gzip") }
            assertEquals(ContentType.Image.PNG, image.contentType()?.withoutParameters())
            assertNull(image.headers[HttpHeaders.ContentEncoding])
            assertContentEquals(fixture.image, image.bodyAsBytes())

            val session = client.post("/api/v1/development-session") {
                trustedHost()
                header("X-Astrid-Client", "playground")
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
            val token = session.headers[HttpHeaders.SetCookie]
                ?.substringAfter("$SESSION_COOKIE=")
                ?.substringBefore(';')
                ?: error("Session cookie missing")
            val catalog = client.get("/api/v1/catalog") {
                encodedWith("gzip")
                cookie(SESSION_COOKIE, token)
            }
            assertEquals(HttpStatusCode.OK, catalog.status)
            assertTrue(catalog.bodyAsText().toByteArray().size >= STATIC_COMPRESSION_MIN_BYTES)
            assertNull(catalog.headers[HttpHeaders.ContentEncoding])
            assertNull(catalog.headers[HttpHeaders.Vary])
            assertEquals("no-store", catalog.headers[HttpHeaders.CacheControl])

            val missing = client.get("/assets/missing.js") { encodedWith("gzip") }
            assertEquals(HttpStatusCode.NotFound, missing.status)
            assertNull(missing.headers[HttpHeaders.ContentEncoding])
            assertNull(missing.headers[HttpHeaders.Vary])
        }
    }

    @Test
    fun `compression remains response only and does not decode request bodies`() = testApplication {
        application { astridModule(config(), InMemoryAstridRepository()) }

        val plain = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, plain.status)
        assertTrue(plain.headers[HttpHeaders.SetCookie]?.startsWith("$SESSION_COOKIE=") == true)

        val encoded = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            header(HttpHeaders.ContentEncoding, "gzip")
            contentType(ContentType.Application.Json)
            setBody("{}".toByteArray().gzip())
        }
        assertEquals(HttpStatusCode.BadRequest, encoded.status)
        assertNull(encoded.headers[HttpHeaders.SetCookie])
        assertEquals("no-store", encoded.headers[HttpHeaders.CacheControl])
    }

    private fun withFixture(block: (StaticFixture) -> Unit) {
        val root = Files.createTempDirectory("astrid-compression-test").toFile()
        try {
            val assets = root.resolve("assets").apply { mkdirs() }
            val fixture = StaticFixture(root)
            root.resolve("index.html").writeBytes(fixture.index)
            assets.resolve("app.js").writeBytes(fixture.script)
            assets.resolve("app.css").writeBytes(fixture.style)
            assets.resolve("tiny.js").writeBytes(fixture.tinyScript)
            assets.resolve("inter.woff2").writeBytes(fixture.font)
            assets.resolve("poster.png").writeBytes(fixture.image)
            block(fixture)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun assertVariesByAcceptEncoding(response: HttpResponse) {
        val values = response.headers.getAll(HttpHeaders.Vary).orEmpty().flatMap { it.split(',') }
        assertTrue(values.any { it.trim().equals(HttpHeaders.AcceptEncoding, ignoreCase = true) })
    }

    private fun assertJavaScript(contentType: ContentType?) {
        val type = contentType?.withoutParameters()
        assertTrue(type == ContentType.Application.JavaScript || type == ContentType.Text.JavaScript)
    }

    private fun ByteArray.gunzip() = GZIPInputStream(ByteArrayInputStream(this)).use { it.readBytes() }

    private fun ByteArray.gzip(): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(this) }
        return output.toByteArray()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.trustedHost() {
        header(HttpHeaders.Host, "localhost:8088")
    }

    private fun io.ktor.client.request.HttpRequestBuilder.encodedWith(value: String) {
        trustedHost()
        header(HttpHeaders.AcceptEncoding, value)
    }

    private fun config(webStaticDir: String? = null) = ServerConfig(
        developmentMode = true,
        inspectorEnabled = false,
        inspectorKey = null,
        secureCookie = false,
        trustedForwardedHeaders = false,
        allowedOrigins = setOf("http://localhost:8088"),
        allowedHosts = setOf("localhost:8088"),
        sessionTtlMillis = 60_000,
        webStaticDir = webStaticDir,
        jdbcUrl = "jdbc:postgresql://unused",
        r2dbcUrl = "r2dbc:postgresql://unused",
        postgresUser = "unused",
        postgresPassword = "unused",
        poolMaxSize = 2,
    )

    private data class StaticFixture(val root: java.io.File) {
        val index = ("<!doctype html><main>Astrid shell</main>" + "<!-- shell -->".repeat(200)).toByteArray()
        val script = ("window.astridLoaded = true;\n" + "console.log('astrid');\n".repeat(200)).toByteArray()
        val style = ("body { color: #8f173b; }\n" + ".lesson { display: grid; }\n".repeat(200)).toByteArray()
        val tinyScript = "window.tiny = true;".toByteArray()
        val font = ByteArray(4_096) { (it % 251).toByte() }
        val image = ByteArray(4_096) { (it % 239).toByte() }
    }

    private companion object {
        const val SESSION_COOKIE = "astrid_session"
    }
}
