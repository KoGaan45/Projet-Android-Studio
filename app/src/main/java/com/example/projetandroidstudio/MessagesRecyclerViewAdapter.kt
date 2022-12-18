package com.example.projetandroidstudio

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MessagesRecyclerViewAdapter(var mValues: ListeMessages?) : RecyclerView.Adapter<MessagesRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(val mView: View) : RecyclerView.ViewHolder(
        mView
    ) {
        val mContentView: TextView
        var mItem: Message? = null

        init {
            mContentView = mView.findViewById<View>(R.id.contenuMessage) as TextView
        }

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var date = SimpleDateFormat("dd/MM HH:mm").format(mValues!!.get(position)!!.date)

        holder.mItem = mValues!!.get(position)
        var textContent: String = mValues!!.get(position)!!.contenu
        var sourceString = "<b>" + mValues!!.get(position)!!.auteur + " (" + date + "):</b> " + textContent;
        holder.mContentView.text = Html.fromHtml(sourceString)
        holder.mView.setOnClickListener { v: View? ->

        }
    }

    override fun getItemCount(): Int {
        return mValues!!.size()
    }

    fun ajouteMessage(id: Int, date: Date?, author: String?, contenu: String?) {
        mValues!!.ajouteMessage(id, date, author, contenu)
        this.notifyItemInserted(mValues!!.size() - 1)
    }
}