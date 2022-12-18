package com.example.projetandroidstudio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class MessagesRecyclerViewAdapter(var mValues: ListeMessages?) : RecyclerView.Adapter<MessagesRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(val mView: View) : RecyclerView.ViewHolder(
        mView
    ) {
        val mAuteurview: TextView
        val mDateMessage: TextView
        val mContentView: TextView
        var mItem: Message? = null

        init {
            mAuteurview = mView.findViewById<View>(R.id.auteurMessage) as TextView
            mDateMessage = mView.findViewById<View>(R.id.dateMessage) as TextView
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
        holder.mItem = mValues!!.get(position)
        holder.mAuteurview.text = mValues!!.get(position)!!.auteur
        var textContent: String = mValues!!.get(position)!!.contenu
        //if (textContent.length > 50) textContent = textContent.substring(0, 47) + "..."
        holder.mContentView.text = textContent
        holder.mDateMessage.text = mValues!!.get(position)!!.date.toString()
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