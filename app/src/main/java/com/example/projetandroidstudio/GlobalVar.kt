package com.example.projetandroidstudio

import android.app.Application

class GlobalVar : Application() {
    // Utilisation de variable globale pour éviter les magics strings et changer la valeur une seule fois en cas de modification
    companion object {
        const val STATUT_JOUEUR_MORT: String = "DEAD"
        const val STATUT_JOUEUR_EN_VIE: String = "UP"
        const val STATUT_JOUEUR_PREPARATION: String = "PACK"
        const val STATUT_JOUEUR_VOYAGE: String = "VOY"
        const val STATUT_JOUEUR_NETTOYAGE: String = "UP"

        const val TYPE_MARKER_CIBLE = "CIBLE"
        const val TYPE_MARKER_ENNEMI = "ENNEMI"
        const val TYPE_MARKER_ME = "ME"
    }
}