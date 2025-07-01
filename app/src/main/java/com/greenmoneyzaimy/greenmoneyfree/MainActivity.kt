package com.greenmoneyzaimy.greenmoneyfree

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@SuppressLint("SetJavaScriptEnabled")
class MainActivity : Activity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Основной вертикальный контейнер
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Фиксированная заглушка сверху (зеленая полоса)
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(40) // Фиксированная высота 24dp (можно регулировать)
            ).also {
                it.height = dpToPx(40) // Дублируем для надежности
            }
            setBackgroundColor(Color.parseColor("#FFFFFFFF")) // Зеленый цвет как в теме
        }.also { mainContainer.addView(it) }

        // WebView занимает всё оставшееся пространство
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f // Вес 1 для заполнения пространства
            )
        }.also { mainContainer.addView(it) }

        setContentView(mainContainer)

        // Запускаем загрузку URL
        Thread {
            loadUrlFromServer()
        }.start()
    }

    // Конвертация dp в пиксели
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun loadUrlFromServer() {
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL("https://gm-admin.ru/get_url")
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"

            val inputStream = urlConnection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val response = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }

            val jsonResponse = JSONObject(response.toString())
            val targetUrl = jsonResponse.getString("url")
            val utmSource = jsonResponse.optString("utmSource", "")
            val utmMedium = jsonResponse.optString("utmMedium", "")
            val utmCampaign = jsonResponse.optString("utmCampaign", "")

            val fullUrl = buildFullUrl(targetUrl, utmSource, utmMedium, utmCampaign)

            runOnUiThread {
                webView.loadUrl(fullUrl)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                webView.loadUrl("about:blank")
            }
        } finally {
            urlConnection?.disconnect()
        }
    }

    private fun buildFullUrl(baseUrl: String, source: String, medium: String, campaign: String): String {
        return if (source.isNotEmpty() || medium.isNotEmpty() || campaign.isNotEmpty()) {
            val utmParams = listOf(
                if (source.isNotEmpty()) "utm_source=$source" else null,
                if (medium.isNotEmpty()) "utm_medium=$medium" else null,
                if (campaign.isNotEmpty()) "utm_campaign=$campaign" else null
            ).filterNotNull().joinToString("&")

            if (baseUrl.contains("?")) {
                "$baseUrl&$utmParams"
            } else {
                "$baseUrl?$utmParams"
            }
        } else {
            baseUrl
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}