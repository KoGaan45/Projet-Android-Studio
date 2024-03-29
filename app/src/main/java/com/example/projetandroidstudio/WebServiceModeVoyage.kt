package com.example.projetandroidstudio

import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class WebServiceModeVoyage {
    private val TAG = "WSModeVoyage"

    fun call(session : Int, signature : Long) : Boolean? {
        return try {
            val url = URL("http://51.68.124.144/nettoyeurs_srv/mode_voyage.php?session=$session&signature=$signature")
            val cnx: URLConnection = url.openConnection()
            val `in`: InputStream = cnx.getInputStream()
            val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
            val db: DocumentBuilder = dbf.newDocumentBuilder()
            val xml: Document = db.parse(`in`)
            var nl: NodeList = xml.getElementsByTagName("STATUS")
            val nodeStatus: Node = nl.item(0)
            val status: String = nodeStatus.getTextContent()
            Log.d(TAG, "Thread Mode Voyage: status $status")
            if (!status.startsWith("OK")) return false
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}