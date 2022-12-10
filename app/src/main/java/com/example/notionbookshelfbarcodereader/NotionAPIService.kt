package com.example.notionbookshelfbarcodereader

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

// TODO: 酷すぎるので修正したいなあ
// クラスの作り方わからん
// データクラスわからん
// ネストしたインスタンスの作り方わからん
// @SerializedNameよくわからん
// retrofitわからん
interface NotionAPIService {
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json",
        "Notion-Version: 2022-06-28",
    )
    @POST("pages")
    fun createPages(
        @Header("Authorization") authorization: String,
        @Body body: CreatePagesRequestBody,
    ): Call<ResponseBody>
}

class CreatePagesRequestBody(title: String, genre: String, author: String, publisher: String, databaseId: String) {
    private var parent: Parent? = null
    private var properties: Properties? = null

    init {
        this.parent = Parent(databaseId)
        val titleText = Text(Content(title))
        val titleList = mutableListOf<Text>()
        titleList.add(titleText)
        val titleData = Title(titleList)
        val genreData = Select(Name(genre))
        val authorText = Text(Content(author))
        val authorList =mutableListOf<Text>()
        authorList.add(authorText)
        val authorData = RichText(authorList)
        val publisherData = Select(Name(publisher))
        this.properties = Properties(titleData, genreData, authorData, publisherData)
    }
}

data class Parent (
    @SerializedName("database_id") var databaseId: String,
)

data class Properties (
    @SerializedName("タイトル") val title: Title,
    @SerializedName("ジャンル") val genre: Select,
    @SerializedName("著者") val writer: RichText,
    @SerializedName("出版社") val publisher: Select,
)

data class Title (
    val title: List<Text>,
)

data class RichText (
    @SerializedName("rich_text") var richText: List<Text>,
)

data class Text (
    var text: Content,
) {
    // これなんでいるんだ？ -> listでinitializeするのにいる？
//    init {
//        text = Content("text")
//    }
}

data class Content (
    val content: String,
)

data class Select (
    var select: Name,
)

data class Name (
    var name: String,
)


