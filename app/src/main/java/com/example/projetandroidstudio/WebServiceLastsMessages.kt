package com.example.projetandroidstudio

import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList

class WebServiceLastsMessages {
    val TAG = "WSLastMessages"

    /// à ne pas exécuter dans le thread principal
    fun call(session : Int, signature : Long): ArrayList<Message?>? {
        return try {
            val url = URL("http://51.68.124.144/nettoyeurs_srv/last_msgs.php?session=$session&signature=$signature")
            val cnx: URLConnection = url.openConnection()
            val `in`: InputStream = cnx.getInputStream()
            val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
            val db: DocumentBuilder = dbf.newDocumentBuilder()
            val xml: Document = db.parse(`in`)
            var nl: NodeList = xml.getElementsByTagName("STATUS")
            val nodeStatus: Node = nl.item(0)
            val status: String = nodeStatus.getTextContent()
            Log.d(TAG, "Thread last msg : status $status")
            if (!status.startsWith("OK")) return null
            nl = xml.getElementsByTagName("CONTENT")
            val nodeContent: Node = nl.item(0)
            val messagesXML: NodeList = nodeContent.getChildNodes()
            val aAjouter = ArrayList<Message?>()
            for (i in 0 until messagesXML.getLength()) {
                val message: Node = messagesXML.item(i)
                aAjouter.add(parseMessage(message))
            }
            aAjouter
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseMessage(msgNode: Node): Message? {
        var id = -1
        var auteur: String? = null
        var contenu: String? = null
        var stringDate: String? = null
        val messageFields: NodeList = msgNode.getChildNodes()
        for (j in 0 until messageFields.getLength()) {
            val field: Node = messageFields.item(j)
            if (field.getNodeName().equals("ID", ignoreCase = true)) id =
                field.getTextContent().toInt() else if (field.getNodeName()
                    .equals("DATESENT", ignoreCase = true)
            ) stringDate = field.getTextContent() else if (field.getNodeName()
                    .equals("AUTHOR", ignoreCase = true)
            ) auteur = field.getTextContent() else if (field.getNodeName()
                    .equals("MSG", ignoreCase = true)
            ) contenu = field.getTextContent()
        }
        val formatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        assert(stringDate != null)
        var date: Date? = null
        date = try {
            formatter.parse(stringDate)
        } catch (e: ParseException) {
            e.printStackTrace()
            return null
        }
        assert(auteur != null)
        assert(contenu != null)
        assert(date != null)
        return Message(id, auteur!!, contenu!!, date)
    }
}