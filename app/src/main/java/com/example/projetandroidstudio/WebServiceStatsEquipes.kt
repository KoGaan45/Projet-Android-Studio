package com.example.projetandroidstudio

import android.location.Location
import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class WebServiceStatsEquipes {
    private val TAG = "WSStatsEquipes"

    fun call(session : Int, signature : Long) : ArrayList<String>? {
        return try {
            val url = URL("http://51.68.124.144/nettoyeurs_srv/stats_equipe.php?session=$session&signature=$signature")
            val cnx: URLConnection = url.openConnection()
            val `in`: InputStream = cnx.getInputStream()
            val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
            val db: DocumentBuilder = dbf.newDocumentBuilder()
            val xml: Document = db.parse(`in`)
            var nl: NodeList = xml.getElementsByTagName("STATUS")
            val nodeStatus: Node = nl.item(0)
            val status: String = nodeStatus.getTextContent()

            Log.d(TAG, "Thread Connexion : status $status")
            if (!status.startsWith("OK")) return null

            nl = xml.getElementsByTagName("PARAMS")
            val nodeContent: Node = nl.item(0)
            val messagesXML: NodeList = nodeContent.getChildNodes()

            val resultat = ArrayList<String>()

            resultat.add(messagesXML.item(0).textContent.toString())
            resultat.add(messagesXML.item(1).textContent.toString())
            resultat.add(messagesXML.item(2).textContent.toString())

            return resultat
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}