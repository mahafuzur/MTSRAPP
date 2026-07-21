package com.youngone.mtsr01

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    // Holds the pending file-chooser callback while the system file picker is open
    // (used by the app's "Load JSON" / "Import CSV" buttons).
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val uris: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK && data?.data != null) {
            arrayOf(data.data!!)
        } else {
            null
        }
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true          // required for localStorage persistence
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        // Exposes window.AndroidSave.saveFile(base64, filename, mime) to the page,
        // used by the "Save JSON" / "Export CSV" buttons to write into Downloads.
        webView.addJavascriptInterface(AndroidSaveBridge(this), "AndroidSave")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                filePathCb: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                filePathCallback = filePathCb
                val intent = fileChooserParams?.createIntent()
                    ?: Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (e: Exception) {
                    filePathCallback = null
                    false
                }
            }
        }

        webView.loadUrl("file:///android_asset/www/index.html")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    /** JavaScript-callable bridge that writes exported files to the device's Downloads folder. */
    class AndroidSaveBridge(private val activity: MainActivity) {
        @JavascriptInterface
        fun saveFile(base64Data: String, filename: String, mime: String) {
            activity.runOnUiThread {
                try {
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val resolver = activity.contentResolver
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, filename)
                            put(MediaStore.Downloads.MIME_TYPE, mime)
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                            values.clear()
                            values.put(MediaStore.Downloads.IS_PENDING, 0)
                            resolver.update(uri, values, null, null)
                            Toast.makeText(activity, "Saved to Downloads: $filename", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(activity, "Could not save $filename", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Pre-Android 10: write to the app's own Downloads folder (no permission needed).
                        val dir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        val file = File(dir, filename)
                        FileOutputStream(file).use { it.write(bytes) }
                        Toast.makeText(activity, "Saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(activity, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
