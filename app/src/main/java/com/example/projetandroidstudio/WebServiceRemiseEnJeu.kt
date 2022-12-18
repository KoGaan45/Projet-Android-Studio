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

class WebServiceRemiseEnJeu {
    val TAG = "WSRemiseEnJeu"

    fun call(session : Int, signature : Long, loc : Location): Boolean {
        return try {
            val url = URL("http://51.68.124.144/nettoyeurs_srv/remise_en_jeu.php?session=$session&signature=$signature&lon=${loc.longitude}&lat=${loc.latitude}")
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