package com.example.projetandroidstudio

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StatPersoActivity : AppCompatActivity() {
    private lateinit var joueur: Joueur
    private lateinit var statutNettoyeur: TextView
    private lateinit var nomNettoyeur: TextView
    private lateinit var valeurNettoyeur: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stat_perso)

        if (savedInstanceState == null) {
            val extras = intent.extras
            if (extras != null) {
                joueur = Joueur(extras.getInt("session"),extras.getLong("signature"),null, null, null, null)
            }
        } else {
            joueur = Joueur(
                savedInstanceState.getSerializable("session") as Int,
                savedInstanceState.getSerializable("signature") as Long,
                null, null, null, null
            )
        }

        statutNettoyeur = findViewById(R.id.StatutNettoyeur)
        nomNettoyeur = findViewById(R.id.NomNettoyeur)
        valeurNettoyeur = findViewById(R.id.ValeurNettoyeur)

        getStatJoueur()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putSerializable("session", joueur.session)
        savedInstanceState.putSerializable("signature", joueur.signature)
    }

    private fun getStatJoueur()
    {
        Thread {
            val ws = WebServiceStatsNettoyeur()
            joueur = ws.call(joueur.session, joueur.signature) ?: return@Thread

            runOnUiThread {
                statutNettoyeur.text = joueur.statut
                valeurNettoyeur.text = joueur.value
                nomNettoyeur.text = joueur.nettoyeur
            }
        }.start()
    }

    fun retour(view: View)
    {
        finish()
    }
}