package app.calsnap.android.presentation.screens.original

import android.app.Activity
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Message
import android.provider.MediaStore
import android.util.Base64
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.FileChooserParams
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import app.calsnap.android.BuildConfig
import java.io.File

private const val CALSNAP_URL = "https://rjv-vi.github.io/CalSnap/"

@Composable
fun OriginalCalSnapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = pendingFileCallback ?: return@rememberLauncherForActivityResult
        pendingFileCallback = null
        val uris = if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            when {
                data?.clipData != null -> Array(data.clipData!!.itemCount) { idx -> data.clipData!!.getItemAt(idx).uri }
                data?.data != null -> arrayOf(data.data!!)
                cameraCaptureUri != null -> arrayOf(cameraCaptureUri!!)
                else -> WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
            }
        } else {
            null
        }
        callback.onReceiveValue(uris)
        cameraCaptureUri = null
    }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { appContext ->
            WebView(appContext).apply {
                webView = this
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.rgb(242, 240, 235))
                overScrollMode = WebView.OVER_SCROLL_NEVER
                isHapticFeedbackEnabled = true

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
                addJavascriptInterface(CalSnapAndroidBridge(appContext), "CalSnapAndroid")

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                    allowContentAccess = true
                    loadsImagesAutomatically = true
                    useWideViewPort = true
                    loadWithOverviewMode = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    javaScriptCanOpenWindowsAutomatically = true
                    setSupportMultipleWindows(true)
                    userAgentString = "$userAgentString CalSnapAndroid/${BuildConfig.VERSION_NAME}"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url
                        if (url.host == "rjv-vi.github.io" && url.path.orEmpty().startsWith("/CalSnap")) return false
                        openExternal(appContext, url)
                        return true
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        canGoBack = view.canGoBack()
                        view.evaluateJavascript(BLOB_DOWNLOAD_BRIDGE, null)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        request.grant(request.resources)
                    }

                    override fun onShowFileChooser(
                        view: WebView,
                        filePathCallback: ValueCallback<Array<Uri>>,
                        fileChooserParams: FileChooserParams,
                    ): Boolean {
                        pendingFileCallback?.onReceiveValue(null)
                        pendingFileCallback = filePathCallback

                        val acceptsImage = fileChooserParams.acceptTypes.any { it.contains("image", ignoreCase = true) } || fileChooserParams.acceptTypes.isEmpty()
                        val pickIntent = fileChooserParams.createIntent().apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                        }
                        val initialIntents = if (acceptsImage) {
                            createImageCaptureIntent(appContext)?.also { cameraCaptureUri = it.outputUri }?.let { arrayOf(it.intent) }.orEmpty()
                        } else {
                            emptyArray()
                        }
                        val chooser = Intent.createChooser(pickIntent, fileChooserParams.title ?: "CalSnap").apply {
                            putExtra(Intent.EXTRA_INITIAL_INTENTS, initialIntents)
                        }
                        return runCatching {
                            filePickerLauncher.launch(chooser)
                            true
                        }.getOrElse {
                            pendingFileCallback = null
                            filePathCallback.onReceiveValue(null)
                            false
                        }
                    }

                    override fun onCreateWindow(
                        view: WebView,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message,
                    ): Boolean {
                        val popup = WebView(view.context)
                        popup.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                openExternal(appContext, request.url)
                                popup.destroy()
                                return true
                            }
                        }
                        (resultMsg.obj as WebViewTransport).webView = popup
                        resultMsg.sendToTarget()
                        return true
                    }
                }

                setDownloadListener(CalSnapDownloadListener(appContext))
                loadUrl(CALSNAP_URL)
            }
        },
        update = { view ->
            canGoBack = view.canGoBack()
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }
}

private data class CaptureIntent(val intent: Intent, val outputUri: Uri)

private fun createImageCaptureIntent(context: Context): CaptureIntent? {
    val imageFile = runCatching {
        File.createTempFile("calsnap_capture_", ".jpg", context.cacheDir)
    }.getOrNull() ?: return null
    val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", imageFile)
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
    return if (intent.resolveActivity(context.packageManager) != null) CaptureIntent(intent, uri) else null
}

private fun openExternal(context: Context, uri: Uri) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private class CalSnapDownloadListener(private val context: Context) : DownloadListener {
    override fun onDownloadStart(url: String, userAgent: String, contentDisposition: String, mimetype: String, contentLength: Long) {
        if (url.startsWith("blob:", ignoreCase = true)) return
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
        val request = DownloadManager.Request(Uri.parse(url))
            .setMimeType(mimetype)
            .addRequestHeader("User-Agent", userAgent)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }
}

private class CalSnapAndroidBridge(private val context: Context) {
    @JavascriptInterface
    fun saveBase64File(fileName: String, mimeType: String, base64: String) {
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName.ifBlank { "calsnap-export" })
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(Base64.decode(base64, Base64.DEFAULT))
            }
        }
    }
}

private const val BLOB_DOWNLOAD_BRIDGE = """
(function(){
  if (window.__calSnapAndroidBlobBridge) return;
  window.__calSnapAndroidBlobBridge = true;
  const oldCreate = URL.createObjectURL.bind(URL);
  const blobs = {};
  URL.createObjectURL = function(blob) {
    const url = oldCreate(blob);
    blobs[url] = blob;
    return url;
  };
  const oldClick = HTMLAnchorElement.prototype.click;
  HTMLAnchorElement.prototype.click = function() {
    try {
      if (this.href && this.href.indexOf('blob:') === 0 && window.CalSnapAndroid) {
        const blob = blobs[this.href];
        if (blob) {
          const reader = new FileReader();
          const name = this.download || 'calsnap-export';
          reader.onloadend = function() {
            const result = String(reader.result || '');
            const base64 = result.indexOf(',') >= 0 ? result.split(',')[1] : result;
            window.CalSnapAndroid.saveBase64File(name, blob.type || 'application/octet-stream', base64);
          };
          reader.readAsDataURL(blob);
          return;
        }
      }
    } catch(e) {}
    oldClick.call(this);
  };
})();
"""
