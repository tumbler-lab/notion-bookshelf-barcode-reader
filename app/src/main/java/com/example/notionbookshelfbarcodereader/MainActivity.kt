package com.example.notionbookshelfbarcodereader
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.notionbookshelfbarcodereader.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.common.Barcode
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var codeScanner: CodeScanner
    private val launcher = registerForActivityResult(
        CameraPermission.RequestContract(), ::onPermissionResult
    )
    private val issNdlRetrofit = Retrofit.Builder().apply {
        baseUrl("https://iss.ndl.go.jp/")
    }.build()
//    private val notionRetrofit = Retrofit.Builder().apply {
//        baseUrl("https://api.notion.com/v1/")
//    }.build()
    private val issNdlService = issNdlRetrofit.create(IssNdlService::class.java) // NOTE: javaのクラス情報を渡す必要があるため
//    private val notionAPIService = notionRetrofit.create(NotionAPIService::class.java) // NOTE: javaのクラス情報を渡す必要があるため
    private var isOpenDialog = false // dialogが開いているかのフラグ
    private var secretToken = ""
    private var databaseId = ""
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT > 9) {
            // NOTE: これがないと通信できなさそう？
            // https://stackoverflow.com/questions/25093546/android-os-networkonmainthreadexception-at-android-os-strictmodeandroidblockgua
            val policy = ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }
        codeScanner = CodeScanner(this, binding.previewView, ::onDetectCode)

        if (CameraPermission.hasPermission(this)) {
            startScanner()
        } else {
            launcher.launch(Unit)
        }
        // edit text setting
        val editTextSecretToken = findViewById<EditText>(R.id.editTextSecretToken)
        val editTextDatabaseId = findViewById<EditText>(R.id.editTextDatabaseId)

        editTextSecretToken.setOnEditorActionListener(OnEditorActionListener { v, actionId, _ ->
            when(actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    secretToken = v.text.toString()

                    true
                }
                else -> false
            }
        })
        editTextDatabaseId.setOnEditorActionListener(OnEditorActionListener { v, actionId, _ ->
            when(actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    databaseId = v.text.toString()

                    true
                }
                else -> false
            }
        })
    }

    private fun onPermissionResult(granted: Boolean) {
        if (granted) {
            startScanner()
        } else {
            finish()
        }
    }

    private fun startScanner() {
        codeScanner.start()
    }

    private fun onDetectCode(codes: List<Barcode>) {
        if (isOpenDialog) return
        for (code in codes) {
            println("raw value ${code.rawValue}")
            if (isIsbn(code.rawValue as String)) {
                val responseBody = fetchBookInfo(code.rawValue as String).body()
                parseXml(responseBody)
                // NOTE: parse xmlしたらcodesをリセットしたい->もうなってる？
                break
            }
        }
    }
    private fun isIsbn(code: String): Boolean {
        if (code.length != 13) return false
        return  code.substring(0, 3) == "978"
    }
    private fun fetchBookInfoFromIsbn(query: String): Call<ResponseBody> {
        return issNdlService.fetchBookInfo("searchRetrieve", query, "xml")
    }

    private fun fetchBookInfo(isbn: String): Response<ResponseBody> {
        val query = """
            isbn="$isbn"
        """.trimIndent()
        val get = fetchBookInfoFromIsbn(query)

        return get.execute()
    }
    private fun parseXml(responseBody: ResponseBody?) {
        val issNdlBookInfoList: List<IssNdlXmlParser.IssNdlBookInfo>? = responseBody?.byteStream()?.use { stream ->
            println("stream: $stream")
            IssNdlXmlParser().parse(stream)
        }
        println("entries")
        if (issNdlBookInfoList?.firstOrNull()?.title != null) {
            createConfirmDialog(issNdlBookInfoList.firstOrNull())
        }
        // TODO: add error handling
    }

//    private fun postCreatePages(requestBody: RequestBody): Call<ResponseBody> {
//        return notionAPIService.createPages(
//            "",
//            "",
//            "",
//            "",
//            requestBody,
//        )
//    }

//    private fun postCreatePagesFromBookInfo(bookInfo: IssNdlXmlParser.IssNdlBookInfo?): Response<ResponseBody> {
//        val requestBody: RequestBody
//        val get = postCreatePages(requestBody)
//
//        return get.execute()
//    }

    private fun createConfirmDialog(bookInfo: IssNdlXmlParser.IssNdlBookInfo?) {
        isOpenDialog = true
        val alertDialog: AlertDialog = this@MainActivity.let {
            val title = bookInfo?.title
            val builder = AlertDialog.Builder(it)
            builder.setTitle("notionにこの本の情報を登録しますか？${title}")
            builder.apply {
                setPositiveButton(android.R.string.ok,
                    DialogInterface.OnClickListener { dialog, _ ->
                        // User clicked OK button
                        Log.d(TAG, "click ok button, book title: $title")
                        Log.d(TAG, "secret is $secretToken, database id is $databaseId")
                        dialog.dismiss()
                        // NOTE: dialogを閉じるのでscan再開
                        isOpenDialog = false
                    })
                setNegativeButton(android.R.string.cancel,
                    DialogInterface.OnClickListener { dialog, _ ->
                        // User cancelled the dialog
                        Log.d(TAG, "click cancel button, book title: $title")
                        dialog.cancel()
                        // NOTE: dialogを閉じるのでscan再開
                        isOpenDialog = false
                    })
            }
            // Set other dialog properties


            // Create the AlertDialog
            builder.create()
        }
        alertDialog.show()
    }
}