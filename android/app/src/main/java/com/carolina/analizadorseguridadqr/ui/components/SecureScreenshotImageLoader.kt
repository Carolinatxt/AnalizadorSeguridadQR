package com.carolina.analizadorseguridadqr.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

private const val ALLOWED_SCREENSHOT_IMAGE_HOST = "app.snap-render.com"

private class ScreenshotHostAllowlistInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestUrl = request.url

        // Defensa en profundidad: solo permitimos la URL firmada esperada
        // de SnapRender y exigimos HTTPS para evitar cargas remotas arbitrarias.
        val isAllowedRequest =
            requestUrl.isHttps && requestUrl.host == ALLOWED_SCREENSHOT_IMAGE_HOST

        if (!isAllowedRequest) {
            throw SecurityException("Blocked screenshot image host")
        }

        return chain.proceed(request)
    }
}

private fun createSecureScreenshotOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(ScreenshotHostAllowlistInterceptor())
        .build()
}

private fun createSecureScreenshotImageLoader(
    context: android.content.Context,
    okHttpClient: OkHttpClient,
): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = { okHttpClient },
                )
            )
        }
        .build()
}

@Composable
fun rememberSecureScreenshotImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        val secureOkHttpClient = createSecureScreenshotOkHttpClient()
        createSecureScreenshotImageLoader(
            context = context,
            okHttpClient = secureOkHttpClient,
        )
    }
}
