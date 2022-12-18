package com.example.projetandroidstudio

import java.util.*

class ListeMessages {
    var mMessages = ArrayList<Message>()

    fun ajouteMessage(id: Int, date: Date?, author: String?, message: String?) {
        mMessages.add(Message(id, author!!, message!!, date))
    }

    operator fun get(i: Int): Message? {
        return mMessages[i]
    }

    fun size(): Int {
        return mMessages.size
    }
}