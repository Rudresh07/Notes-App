package com.example.notessqlite

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class NotesAdapter(private var notes : List<Notes>,context: Context):
    RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

        private val db :NotesDatabaseHelper = NotesDatabaseHelper(context)

    class NoteViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {

        val TitleText:TextView = itemView.findViewById(R.id.titleTextView)
        val ContentText:TextView = itemView.findViewById(R.id.NotesContent)
        val UpdateBtn:ImageView = itemView.findViewById(R.id.EditNote)
        val DeleteBtn :ImageView = itemView.findViewById(R.id.DeleteNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item,parent,false)
        return NoteViewHolder(view)
    }

    override fun getItemCount(): Int = notes.size

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.TitleText.text = note.Title
        holder.ContentText.text = note.Content

        holder.UpdateBtn.setOnClickListener {
            val intent = Intent(holder.itemView.context,UpdateNote::class.java).apply {
                putExtra("note_id",note.id)
            }
            holder.itemView.context.startActivity(intent)
        }

        holder.DeleteBtn.setOnClickListener {
            db.deleteNote(note.id)
        refreashData(db.getAllNotes())
            Toast.makeText(holder.itemView.context, "Note Deleted", Toast.LENGTH_SHORT).show()}
    }


    fun refreashData(newNotes:List<Notes>){
        notes = newNotes
        notifyDataSetChanged()
    }


}