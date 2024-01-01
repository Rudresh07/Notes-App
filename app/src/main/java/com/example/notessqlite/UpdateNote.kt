package com.example.notessqlite

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notessqlite.databinding.ActivityMainBinding
import com.example.notessqlite.databinding.ActivityUpdateNoteBinding

class UpdateNote : AppCompatActivity() {

    private lateinit var binding:ActivityUpdateNoteBinding
    private lateinit var db :NotesDatabaseHelper
    private var noteID:Int = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = NotesDatabaseHelper(this)

        noteID = intent.getIntExtra("note_id", -1)
        if (noteID==-1){
            finish()
            return
        }


        val note = db.getNoteByID(noteID)
        binding.EditNotesTitle.setText(note.Title)
        binding.EditNotesContent.setText(note.Content)

        binding.EditNotesubmit.setOnClickListener {
            val newTitle = binding.EditNotesTitle.text.toString()
            var newContent = binding.EditNotesContent.text.toString()
            val updateNote= Notes(noteID,newTitle,newContent)
            db.updateNote(updateNote)
            finish()
            Toast.makeText(this, "Changes Saved", Toast.LENGTH_SHORT).show()

        }

    }
}