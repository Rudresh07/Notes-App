package com.example.notessqlite

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.notessqlite.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: NotesDatabaseHelper
    private lateinit var notesAdapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        db = NotesDatabaseHelper(this)
        notesAdapter = NotesAdapter(db.getAllNotes(), this)

        // Setting layout for recyclerView with 2 columns
        val spanCount = 2
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing) // 8dp spacing

        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.NotesRecyclerView.layoutManager = layoutManager

        val itemDecoration = GridSpacingItemDecoration(spanCount, spacing)
        binding.NotesRecyclerView.addItemDecoration(itemDecoration)

        binding.NotesRecyclerView.adapter = notesAdapter

        binding.AddNewTask.setOnClickListener {
            val intent = Intent(this, AddNotes::class.java)
            startActivity(intent)
        }
    }

    // Refresh data method
    override fun onResume() {
        super.onResume()
        notesAdapter.refreashData(db.getAllNotes())
    }
}