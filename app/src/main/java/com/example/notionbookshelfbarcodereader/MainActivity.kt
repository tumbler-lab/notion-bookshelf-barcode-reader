package com.example.notionbookshelfbarcodereader
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
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
    private val retrofit = Retrofit.Builder().apply {
        baseUrl("https://iss.ndl.go.jp/")
    }.build()
    private val service = retrofit.create(IssNdlService::class.java) // NOTE: javaのクラス情報を渡す必要があるため
    private var isOpenDialog = false // dialogが開いているかのフラグ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT > 9) {
            // これがないと通信できなさそう？
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
                val responseBody = fetchBookInfo(code.rawValue as String)?.body()
                parseXml(responseBody)
                // parse xmlしたらcodesをリセットしたい
                break
            }
        }
    }
    private fun isIsbn(code: String): Boolean {
        if (code.length != 13) return false
        return  code.substring(0, 3) == "978"
    }
    private fun fetchBookInfoFromIsbn(isbn: String): Call<ResponseBody> {
        val query = """
            isbn="$isbn"
        """.trimIndent()
        return service.fetchBookInfo("searchRetrieve", query, "xml")
    }

    private fun fetchBookInfo(isbn: String): Response<ResponseBody> {
        val get = fetchBookInfoFromIsbn(isbn)
        //        println(responseBody.body())
        // xml表示
//        responseBody.body()?.let {
//            println(it.string())
//        }

        return get.execute()
    }
    private fun parseXml(responseBody: ResponseBody?) {
        val issNdlBookInfoList: List<IssNdlXmlParser.IssNdlBookInfo>? = responseBody?.byteStream()?.use { stream ->
            println("stream: $stream")
            IssNdlXmlParser().parse(stream)
        }
        println("entries")
        if (issNdlBookInfoList?.firstOrNull()?.title != null) {
            createConfirmDialog(issNdlBookInfoList?.firstOrNull())
        }
//            val numOfBookInfoProperties
//            val cityWithMaxDegrees = issNdlBookInfoList.maxByOrNull { it. }
            // filterしたかった
//            val filteredIssNdlBookInfoList = issNdlBookInfoList?.filter{ entry ->
//                return@filter (entry?.title != null && entry?.title != "")
//            }
//            val entity = filteredIssNdlBookInfoList?.get(0)
//            createConfirmDialog(entity.toString())

//        } catch (e: XmlPullParserException) {
            // TODO: add error handling
//            println("error in xml parser")
//        }
    }

    private fun postNotion(bookinfo: String) {

    }

    private fun createConfirmDialog(bookInfo: IssNdlXmlParser.IssNdlBookInfo?) {
        isOpenDialog = true
        val alertDialog: AlertDialog? = this@MainActivity.let {
            val title = bookInfo?.title
            val builder = AlertDialog.Builder(it)
            builder.setTitle("notionにこの本の情報を登録しますか？${title}")
            builder.apply {
                setPositiveButton(android.R.string.ok,
                    DialogInterface.OnClickListener { dialog, id ->
                        // User clicked OK button
                        println("click ok button, message: $title")
                        dialog.dismiss()
                        // NOTE: dialogを閉じるのでscan再開
                        isOpenDialog = false
                    })
                setNegativeButton(android.R.string.cancel,
                    DialogInterface.OnClickListener { dialog, id ->
                        // User cancelled the dialog
                        println("click cancel button")
                        dialog.cancel()
                        // NOTE: dialogを閉じるのでscan再開
                        isOpenDialog = false
                    })
            }
            // Set other dialog properties


            // Create the AlertDialog
            builder.create()
        }
        alertDialog?.show()
    }
}