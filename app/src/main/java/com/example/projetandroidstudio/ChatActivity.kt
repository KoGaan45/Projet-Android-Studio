package com.example.projetandroidstudio

import android.content.ContentValues
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatActivity : AppCompatActivity() {

    private var listeMessages = ListeMessages()
    private lateinit var recyclerView : RecyclerView
    private lateinit var joueur : Joueur

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_layout)

        if (savedInstanceState == null) {
            val extras = intent.extras
            if (extras != null) {
                Log.d(ContentValues.TAG,"SESSION = "+extras.getInt("session"))
                joueur = Joueur(extras.getInt("session"),extras.getLong("signature"),extras.getString("nettoyeur"))
            }
        } else {
            joueur = Joueur(savedInstanceState.getSerializable("session") as Int,
                savedInstanceState.getSerializable("signature") as Long, savedInstanceState.getSerializable("nettoyeur") as String?
            )
        }

        recyclerView = findViewById<RecyclerView>(R.id.ChatRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        this.getLastsMessage()
    }

    private fun getLastsMessage()
    {
        Thread {
            val ws = WebServiceLastsMessages()
            val aAjouter: ArrayList<Message?>? = ws.call(joueur.session, joueur.signature)

            try {
                runOnUiThread {
                    for (m in aAjouter!!) {
                        if(!listeMessages.mMessages.contains(m)) listeMessages.ajouteMessage(m!!.id, m!!.date, m!!.auteur, m!!.contenu)
                    }
                    recyclerView.adapter = MessagesRecyclerViewAdapter(listeMessages)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}