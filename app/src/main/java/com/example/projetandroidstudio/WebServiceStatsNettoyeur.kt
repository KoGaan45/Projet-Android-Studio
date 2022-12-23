package com.example.projetandroidstudio

import android.location.Location
import android.provider.Settings.Global
import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class WebServiceStatsNettoyeur {

    private val TAG = "WSStatsNettoyeur"

    fun call(session : Int, signature : Long) : Joueur? {
        return try {
            val url = URL("http://51.68.124.144/nettoyeurs_srv/stats_nettoyeur.php?session=$session&signature=$signature")
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
            Log.d(TAG,messagesXML.item(0).textContent.toString())

            val nbNodes = messagesXML.length

            if(nbNodes == 1){
                // Le nettoyeur est mort
                return Joueur(session, signature, GlobalVar.STATUT_JOUEUR_MORT , null, null, GlobalVar.STATUT_JOUEUR_MORT)
            }

            val etat = messagesXML.item(4).textContent.toString()

            var location = Location("")
            location.longitude = messagesXML.item(2).textContent.toDouble()
            location.latitude = messagesXML.item(3).textContent.toDouble()
            val name = messagesXML.item(0).textContent.toString()
            val value = messagesXML.item(1).textContent.toString()

            return Joueur(session, signature, name, value, location, etat)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}