package com.example.projetandroidstudio

import android.util.Log
import com.google.android.gms.location.LocationServices
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.experimental.and

class WebServiceConnexion {

    private val TAG = "WSConnexion"


    fun getSha256Hash(password: String): String? {
        return try {
            var digest: MessageDigest? = null
            try {
                digest = MessageDigest.getInstance("SHA-256")
            } catch (e1: NoSuchAlgorithmException) {
                e1.printStackTrace()
            }
            digest!!.reset()
            bin2hex(digest!!.digest(password.toByteArray()))
        } catch (ignored: java.lang.Exception) {
            null
        }
    }

    private fun bin2hex(data: ByteArray): String? {
        val hex = StringBuilder(data.size * 2)
        for (b in data) hex.append(String.format("%02x", b and 0xFF.toByte()))
        return hex.toString()
    }

    fun call(login : String, password : String) : Joueur? {
        return try {
            Log.d(TAG,getSha256Hash(password)!!)
            val url = URL("http://51.68.124.144/nettoyeurs_srv/connexion.php?login="+login+"&passwd="+getSha256Hash(password)!!)
            val cnx: URLConnection = url.openConnection()
            val `in`: InputStream = cnx.getInputStream()
            val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
            val db: DocumentBuilder = dbf.newDocumentBuilder()
            val xml: Document = db.parse(`in`)
            var nl: NodeList = xml.getElementsByTagName("STATUS")
            val nodeStatus: Node = nl.item(0)
            val status: String = nodeStatus.getTextContent()
            Log.d(TAG, "Thread connexion : status $status")
            if (!status.startsWith("OK")) return null
            nl = xml.getElementsByTagName("PARAMS")
            val nodeContent: Node = nl.item(0)
            val messagesXML: NodeList = nodeContent.getChildNodes()
            val session : Int = Integer.parseInt(messagesXML.item(0).textContent.toString())
            val signature : Long = Integer.parseInt(messagesXML.item(1).textContent.toString()).toLong()
            return Joueur(session, signature, null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}