package com.example.notessqlite

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notessqlite.databinding.ActivityAddNotesBinding

class AddNotes : AppCompatActivity() {

    private lateinit var binding:ActivityAddNotesBinding
    private lateinit var db:NotesDatabaseHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNotesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        db = NotesDatabaseHelper(this)


        binding.submit.setOnClickListener{
            val title = binding.NotesTitle.text.toString()
            var content = binding.NotesContent.text.toString()
            val note= Notes(0,title,content)
            db.insertNote(note)
            finish()
            Toast.makeText(this, "Note Saved", Toast.LENGTH_SHORT).show()
        }

        }
    }
