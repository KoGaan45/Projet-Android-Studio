package com.example.projetandroidstudio

import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class WebserviceNewMessage(var mContenu: String?) {
    val TAG = "WSNewMessage"

    fun call(session : Int, signature : Long): Boolean {
        try {
            mContenu = URLEncoder.encode(mContenu, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        return if (mContenu == null) false else try {
            val url = URL("http://51.68.124.144/nettoyeurs_srv/new_msg.php?session=$session&signature=$signature&message=$mContenu")
            val cnx: URLConnection = url.openConnection()
            val `in`: InputStream = cnx.getInputStream()
            val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
            val db: DocumentBuilder = dbf.newDocumentBuilder()
            val xml: Document = db.parse(`in`)
            val nl: NodeList = xml.getElementsByTagName("STATUS")
            val nodeStatus: Node = nl.item(0)
            val status: String = nodeStatus.getTextContent()
            Log.d(TAG, "Thread last msg : status $status")
            status.startsWith("OK")
        } catch (e: Exception) {
            false
        }
    }
}