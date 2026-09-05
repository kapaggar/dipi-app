package org.dhamma.dipi.staff.network

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import org.dhamma.dipi.staff.model.SheetExport
import org.dhamma.dipi.staff.model.SheetPayload
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * Binary-sheet `save()` honesty: a 200 HTML body is a refusal, an empty
 * body is "$title came back empty", and the guarded fallback keeps the
 * throwable's own words unless it is an IOException.
 */
class SheetTransportSaveTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var transport: SheetTransport

    @Before
    fun start() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .build()
            .create(StaffApi::class.java)
        transport = SheetTransport(api, server.url("/").toString()) { File(tmp.root, "sheets") }
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun saveTreatsTextHtmlAsRefusalEvenOn200() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody("<html>Access denied</html>"),
        )
        val payload = runBlocking { transport.fetch(SheetExport.Day11Report, 1, 10) }
        assertTrue(payload is SheetPayload.NotAvailable)
        assertEquals("<html>Access denied</html>", (payload as SheetPayload.NotAvailable).message)
    }

    @Test
    fun saveEmptyBodyIsCameBackEmpty() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/pdf")
                .setBody(""),
        )
        val payload = runBlocking { transport.fetch(SheetExport.Day11Report, 1, 10) }
        assertTrue(payload is SheetPayload.NotAvailable)
        assertEquals("Course summary came back empty", (payload as SheetPayload.NotAvailable).message)
    }

    @Test
    fun guardedIoExceptionIsTheOfflineSentence() {
        assertEquals(
            "Offline — could not reach the desk for Laundry list",
            sheetFailureMessage("Laundry list", IOException("timeout")),
        )
    }

    @Test
    fun guardedOtherExceptionSurfacesEMessage() {
        assertEquals(
            "boom",
            sheetFailureMessage("Laundry list", IllegalStateException("boom")),
        )
    }

    /**
     * `@Streaming` returns when headers arrive; `body.bytes()` is still a
     * network read. Pin that the read happens on the IO dispatcher we pass
     * in — on a device that is Main, and that is `NetworkOnMainThreadException`.
     */
    @Test
    fun saveReadsStreamedBodyOnIoDispatcher() {
        val ioExecutor = Executors.newSingleThreadExecutor { Thread(it, "sheet-save-io") }
        val io = ioExecutor.asCoroutineDispatcher()
        val readOn = AtomicReference<String>()
        try {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val resp = chain.proceed(chain.request())
                    val raw = resp.body ?: return@addInterceptor resp
                    resp.newBuilder()
                        .body(
                            object : ResponseBody() {
                                override fun contentType() = raw.contentType()
                                override fun contentLength() = raw.contentLength()
                                override fun source() = object : ForwardingSource(raw.source()) {
                                    override fun read(sink: Buffer, byteCount: Long): Long {
                                        readOn.set(Thread.currentThread().name)
                                        return super.read(sink, byteCount)
                                    }
                                }.buffer()
                            },
                        )
                        .build()
                }
                .build()
            val api = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .build()
                .create(StaffApi::class.java)
            val isolated = SheetTransport(
                api,
                server.url("/").toString(),
                io,
            ) { File(tmp.root, "sheets-io") }
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/pdf")
                    .setBody("%PDF-1.4 fixture"),
            )
            val payload = runBlocking { isolated.fetch(SheetExport.Day11Report, 1, 10) }
            assertTrue(payload is SheetPayload.Document)
            assertTrue(
                "streamed body must be read on the IO dispatcher, was ${readOn.get()}",
                readOn.get().orEmpty().startsWith("sheet-save-io"),
            )
        } finally {
            io.close()
            ioExecutor.shutdown()
        }
    }
}
