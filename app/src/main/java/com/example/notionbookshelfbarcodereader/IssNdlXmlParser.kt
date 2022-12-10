package com.example.notionbookshelfbarcodereader

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

private val ns: String? = null

class IssNdlXmlParser {
    data class IssNdlBookInfo(val title: String?, val subject: String?, val creator : String?, val description: String?, val publisher: String?, val language: String?)

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream): List<IssNdlBookInfo> {
        inputStream.use { inputStream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            return readFeed(parser)
        }
    }
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser): List<IssNdlBookInfo> {

        parser.require(XmlPullParser.START_TAG, ns, "searchRetrieveResponse")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Starts by looking for the entry tag
            println("parser name: ${parser.name}")
            if (parser.name == "records"){
                return readRecords(parser)
            } else {
                skip(parser)
            }
        }
        return mutableListOf<IssNdlBookInfo>()
    }
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readRecords(parser: XmlPullParser): List<IssNdlBookInfo> {
        val entries = mutableListOf<IssNdlBookInfo>()

        parser.require(XmlPullParser.START_TAG, ns, "records")

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Starts by looking for the record tag
            if (parser.name == "record") {
                entries.add(readRecord(parser))
            } else {
                skip(parser)
            }
        }
        return entries
    }
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readRecord(parser: XmlPullParser): IssNdlBookInfo {
        parser.require(XmlPullParser.START_TAG, ns, "record")

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Starts by looking for the record tag
            if (parser.name == "recordData") {
                return readRecordData(parser)
            } else {
                skip(parser)
            }
        }
        return IssNdlBookInfo(null, null, null, null, null, null)
    }
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readRecordData(parser: XmlPullParser): IssNdlBookInfo {
        parser.require(XmlPullParser.START_TAG, ns, "recordData")

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Starts by looking for the record tag
            if (parser.name == "srw_dc:dc") {
                return readEntry(parser)
            } else {
                skip(parser)
            }
        }
        return IssNdlBookInfo(null, null, null, null, null, null)
    }
    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readEntry(parser: XmlPullParser): IssNdlBookInfo {
        parser.require(XmlPullParser.START_TAG, ns, "srw_dc:dc")
        var title: String? = null
        var subject: String? = null
        var creator: String? = null
        var description: String? = null
        var publisher: String? = null
        var language: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            println("readEntry: parser.name: ${parser.name}")
            when (parser.name) {
                "dc:title" -> title = readTag(parser)
                "dc:subject" -> subject = readTag(parser)
                "dc:creator" -> creator = readTag(parser)
                "dc:description" -> description = readTag(parser)
                "dc:publisher" -> publisher = readTag(parser)
                "dc:language" -> language = readTag(parser)
                else -> skip(parser)
            }
        }
        return IssNdlBookInfo(title, subject, creator, description, publisher, language)
    }
    // Processes tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTag(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, parser.name)
        val tag = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, parser.name)
        return tag
    }
    // For the tags title and summary, extracts their text values.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }
    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}