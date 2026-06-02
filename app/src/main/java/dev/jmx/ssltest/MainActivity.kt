package dev.jmx.ssltest

import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.TlsVersion
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var runButton: Button

    private val apiUrls = listOf(
        "https://www.cdnhth.club",
        "https://www.cdnmhwscc.vip",
        "https://www.jmapiproxyxxx.vip",
        "https://www.cdnxxx-proxy.xyz",
        "https://www.jmeadpoolcdn.life"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusText = TextView(this).apply {
            textSize = 12f
            setPadding(16, 16, 16, 16)
        }
        runButton = Button(this).apply {
            text = "开始测试所有 SSL 方案"
            setOnClickListener { runAllTests() }
        }

        val scrollView = android.widget.ScrollView(this)
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(runButton)
            addView(statusText)
        }
        scrollView.addView(layout)
        setContentView(scrollView)
    }

    private fun logMsg(msg: String) {
        runOnUiThread {
            statusText.append("$msg\n")
            android.util.Log.i("SSLTEST", msg)
        }
    }

    private fun runAllTests() {
        statusText.text = ""
        logMsg("=== SSL 握手测试开始 ===")
        logMsg("设备: ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE}")
        logMsg("")

        Thread {
            try {
                val logFile = createLogFile()
                val results = StringBuilder()

                // 方案 1: 默认 OkHttp
                runTest("方案1-默认OkHttp") { createDefaultClient() }
                // 方案 2: TLSv1.2 自定义 SSLSocketFactory
                runTest("方案2-TLSv1.2") { createTls12Client() }
                // 方案 3: TLS 默认协议
                runTest("方案3-TLS默认") { createTlsDefaultClient() }
                // 方案 4: ConnectionSpec MODERN_TLS
                runTest("方案4-ModernTLS") { createModernTlsClient() }
                // 方案 5: ConnectionSpec COMPATIBLE_TLS
                runTest("方案5-CompatTLS") { createCompatTlsClient() }
                // 方案 6: TLSv1.2 + 信任所有证书
                runTest("方案6-TLSv1.2+信任所有") { createTls12TrustAllClient() }
                // 方案 7: TLSv1.3 显式 (如果支持)
                runTest("方案7-TLSv1.3") { createTls13Client() }

                // 写文件
                File(logFile, "ssl_test_results.txt").writeText(results.toString())
                logMsg("\n结果已保存到: ${logFile}/ssl_test_results.txt")
                logMsg("=== 测试完成 ===")
            } catch (e: Exception) {
                logMsg("测试异常: ${e.message}")
            }
        }.start()
    }

    private fun createLogFile(): File {
        val dir = getExternalFilesDir(null) ?: filesDir
        return dir
    }

    private data class TestResult(
        val approachName: String,
        val urlIndex: Int,
        val url: String,
        val success: Boolean,
        val timeMs: Long,
        val error: String = ""
    )

    private fun runTest(name: String, clientFactory: () -> OkHttpClient) {
        logMsg("\n--- $name ---")
        val results = mutableListOf<TestResult>()

        for ((i, baseUrl) in apiUrls.withIndex()) {
            val url = "$baseUrl/promote?_=${System.currentTimeMillis()}"
            val start = System.currentTimeMillis()

            try {
                val client = clientFactory()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val elapsed = System.currentTimeMillis() - start

                if (response.isSuccessful) {
                    logMsg("  URL${i+1}(${baseUrl.replace("https://", "")}): ✅ 成功 (${elapsed}ms) HTTP ${response.code}")
                    results.add(TestResult(name, i, baseUrl, true, elapsed))
                } else {
                    logMsg("  URL${i+1}(${baseUrl.replace("https://", "")}): ⚠️ HTTP ${response.code} (${elapsed}ms)")
                    results.add(TestResult(name, i, baseUrl, false, elapsed, "HTTP ${response.code}"))
                }
                response.close()
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - start
                val msg = when {
                    e.message?.contains("Connection closed by peer") == true -> "连接被服务器关闭(握手拒绝)"
                    e.message?.contains("timeout") == true -> "连接超时"
                    e.message?.contains("cert") == true || e.message?.contains("Certificate") == true -> "证书验证失败"
                    else -> e.message ?: "未知错误"
                }
                logMsg("  URL${i+1}(${baseUrl.replace("https://", "")}): ❌ $msg (${elapsed}ms)")
                results.add(TestResult(name, i, baseUrl, false, elapsed, msg))
            }
        }

        // 统计
        val successCount = results.count { it.success }
        logMsg("  结果: $successCount/${apiUrls.size} 成功")
    }

    // ============ SSL 方案 ============

    /** 方案1: 默认 OkHttp，无任何自定义 */
    private fun createDefaultClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /** 方案2: 显式 TLSv1.2 */
    private fun createTls12Client(): OkHttpClient {
        val trustManager = createDelegatingTrustManager()
        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }

    /** 方案3: TLS 默认协议 */
    private fun createTlsDefaultClient(): OkHttpClient {
        val trustManager = createDelegatingTrustManager()
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }

    /** 方案4: ConnectionSpec MODERN_TLS */
    private fun createModernTlsClient(): OkHttpClient {
        val trustManager = createDelegatingTrustManager()
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
            .build()
    }

    /** 方案5: ConnectionSpec COMPATIBLE_TLS */
    private fun createCompatTlsClient(): OkHttpClient {
        val trustManager = createDelegatingTrustManager()
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .connectionSpecs(listOf(ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
            .build()
    }

    /** 方案6: TLSv1.2 + 信任所有证书 */
    private fun createTls12TrustAllClient(): OkHttpClient {
        val trustAllManager = createTrustAllManager()
        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(null, arrayOf<TrustManager>(trustAllManager), SecureRandom())
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    /** 方案7: 尝试 TLSv1.3 (在支持的设备上) */
    private fun createTls13Client(): OkHttpClient {
        return try {
            val trustManager = createDelegatingTrustManager()
            val sslContext = SSLContext.getInstance("TLSv1.3")
            sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .build()
        } catch (e: Exception) {
            // TLSv1.3 可能不支持，回退到 TLS
            logMsg("  (TLSv1.3 不可用，回退到 TLS 默认: ${e.message})")
            createTlsDefaultClient()
        }
    }

    // ============ TrustManager 工具 ============

    /** 委托到系统默认的 TrustManager，正常验证证书 */
    private fun createDelegatingTrustManager(): X509TrustManager {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as java.security.KeyStore?)
        for (tm in tmf.trustManagers) {
            if (tm is X509TrustManager) return tm
        }
        throw RuntimeException("找不到系统 TrustManager")
    }

    /** 信任所有证书（仅用于测试） */
    @Suppress("CustomX509TrustManager")
    private fun createTrustAllManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }
}