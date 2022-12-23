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

class WebServiceFrappeNettoyeur {
    private val TAG = "WSCreationFrappeNet"

    fun call(session : Int, signature : Long, cibleId: Int) : String? {
        return try {
            val url = URL("http://51.68.124.144/nettoyeurs_srv/frappe_net.php?session=$session&signature=$signature&net_id=$cibleId")

            val cnx: URLConnection = url.openConnection()
            val `in`: InputStream = cnx.getInputStream()

            val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
            val db: DocumentBuilder = dbf.newDocumentBuilder()
            val xml: Document = db.parse(`in`)

            var nl: NodeList = xml.getElementsByTagName("STATUS")
            val nodeStatus: Node = nl.item(0)
            val status: String = nodeStatus.textContent
            Log.d(TAG, "Thread Frappe de nettoyeur : status $status")
            if (!status.startsWith("OK")) return status

            nl = xml.getElementsByTagName("PARAMS")
            val nodeContent: Node = nl.item(0)
            val messagesXML: NodeList = nodeContent.childNodes

            return messagesXML.item(0).textContent.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}