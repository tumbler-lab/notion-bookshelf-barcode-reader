package com.example.notionbookshelfbarcodereader
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.appcompat.app.AppCompatActivity
import com.example.notionbookshelfbarcodereader.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.common.Barcode
import okhttp3.ResponseBody
import org.xmlpull.v1.XmlPullParserException
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.io.InputStream


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
            start()
        } else {
            launcher.launch(Unit)
        }
    }

    private fun onPermissionResult(granted: Boolean) {
        if (granted) {
            start()
        } else {
            finish()
        }
    }

    private fun start() {
        codeScanner.start()
    }

    private fun onDetectCode(codes: List<Barcode>) {
//        codes.forEach {
//            Toast.makeText(this, it.rawValue, Toast.LENGTH_LONG).show()
//        }
        codes.forEach{
            println("raw value ${it.rawValue}")
            val response = fetchBookInfo(it.rawValue as String)
            parseXml(response)
            // parsexmlしたらcodesをリセットしたい
        }
    }
    fun fetchBookInfoFromIsbn(isbn: String): Call<ResponseBody> {
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
    private fun parseXml(response: Response<ResponseBody>) {
//        try {
            val responseBody: ResponseBody? = response?.body()
            val entries: List<IssNdlXmlParser.Entry>? = responseBody?.byteStream()?.use { stream ->
                println("stream: $stream")
                IssNdlXmlParser().parse(stream)
            }
//            val entries: List<IssNdlXmlParser.Entry> = is{ stream ->
//                // Instantiate the parser
//                StackOverflowXmlParser().parse(stream)
//            } ?: emptyList()
            println("entries")
            entries?.forEach{ entry ->
                println(entry)
                if (entry.title != "") {

                }
            }
//        } catch (e: XmlPullParserException) {
            // TODO: add error handling
//            println("error in xml parser")
//        }
    }

    private fun postNotion
}