package com.example.projetandroidstudio

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StatEquipesActivity : AppCompatActivity() {
    private lateinit var joueur: Joueur
    private lateinit var valeurVotreEquipe: TextView
    private lateinit var valeurEquipeAdverse: TextView
    private lateinit var nombreDeNettoyeur: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stat_equipe)

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

        valeurVotreEquipe = findViewById(R.id.ValeurVotreEquipe)
        valeurEquipeAdverse = findViewById(R.id.ValeurEquipeAdverse)
        nombreDeNettoyeur = findViewById(R.id.NombreDeNettoyeur)

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
            val ws = WebServiceStatsEquipes()
            val resultat = ws.call(joueur.session, joueur.signature) ?: return@Thread

            runOnUiThread {
                valeurVotreEquipe.text = resultat[0]
                valeurEquipeAdverse.text = resultat[1]
                nombreDeNettoyeur.text = resultat[2]
            }
        }.start()
    }

    fun retour(view: View)
    {
        finish()
    }
}