package com.example.projetandroidstudio

import java.util.Date

class Message(val id: Int, val auteur: String, val contenu: String, val date: Date?) {

    // La méthode contains d'une arrayList fait appel à equals la surcharger permet de comparer les messages
    override fun equals(other: Any?): Boolean {
        super.equals(other)

        if (other is Message && other.id == this.id) return true

        return false
    }
}