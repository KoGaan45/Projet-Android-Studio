package com.example.projetandroidstudio

import android.content.ContentValues
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask


class ChatActivity : AppCompatActivity() {

    private var listeMessages = ListeMessages()
    private lateinit var recyclerView : RecyclerView
    private lateinit var editText: EditText
    private lateinit var joueur : Joueur
    private lateinit var mainHandler: Handler
    private val updateChat = object : Runnable {
        override fun run() {
            getLastsMessage()
            mainHandler.postDelayed(this, 10000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_layout)

        if (savedInstanceState == null) {
            val extras = intent.extras
            if (extras != null) {
                Log.d(ContentValues.TAG,"SESSION = "+extras.getInt("session"))
                joueur = Joueur(extras.getInt("session"),extras.getLong("signature"),extras.getString("nettoyeur"), null, null, null)
            }
        } else {
            joueur = Joueur(
                savedInstanceState.getSerializable("session") as Int,
                savedInstanceState.getSerializable("signature") as Long,
                savedInstanceState.getSerializable("nettoyeur") as String?,
                null, null, null
            )
        }

        mainHandler = Handler(Looper.getMainLooper())

        editText = findViewById<EditText>(R.id.MessageToSend)

        recyclerView = findViewById<RecyclerView>(R.id.ChatRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        this.getLastsMessage()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putSerializable("session", joueur.session)
        savedInstanceState.putSerializable("signature", joueur.signature)
        savedInstanceState.putSerializable("nettoyeur", joueur.nettoyeur)
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(updateChat)
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(updateChat)
    }

    private fun getLastsMessage()
    {
        Thread {
            val ws = WebServiceLastsMessages()
            val aAjouter: ArrayList<Message?>? = ws.call(joueur.session, joueur.signature)

            try {
                runOnUiThread {
                    for (m in aAjouter!!) {
                        if(m !in listeMessages.mMessages) listeMessages.ajouteMessage(m!!.id, m!!.date, m!!.auteur, m!!.contenu)
                    }

                    listeMessages.mMessages.sortBy { it.id }

                    Log.d("LISTES DES MESSAGES", listeMessages.mMessages.toString())

                    recyclerView.adapter = MessagesRecyclerViewAdapter(listeMessages)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun sendMessage(view: View)
    {
        Thread {
            val ws = WebserviceNewMessage(editText.text.toString())
            val ok: Boolean = ws.call(joueur.session, joueur.signature)
            if (!ok) runOnUiThread {
                Toast.makeText(
                    this,
                    "Erreur dans l'envoi du message",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                runOnUiThread { this.getLastsMessage() }
            }
        }.start()

    }
}