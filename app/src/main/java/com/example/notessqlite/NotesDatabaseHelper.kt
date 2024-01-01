package com.example.notessqlite

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NotesDatabaseHelper (context:Context):SQLiteOpenHelper(context,DATABASE_NAME,null,DATABASE_VERSION) {

    //first we will create all constant
    companion object{

        private const val DATABASE_NAME = "notesapp.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "allnotes"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_CONTENT = "content"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_TITLE TEXT, $COLUMN_CONTENT TEXT)"
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
       val dropTableQuery = "DROP TABLE IF EXISTS $TABLE_NAME"
        db?.execSQL(dropTableQuery)
        onCreate(db)
    }


    fun insertNote(note:Notes){
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, note.Title)
            put(COLUMN_CONTENT, note.Content)
        }

        db.insert(TABLE_NAME,null,values)
        db.close()
    }

    fun getAllNotes():List<Notes>{
        val notesList = mutableListOf<Notes>()
        val db = readableDatabase
        val query = "SELECT* FROM $TABLE_NAME"
        val cursor = db.rawQuery(query,null)

        while(cursor.moveToNext()){
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))

            val note = Notes(id,title,content)
            notesList.add(note)

        }

        cursor.close()
        db.close()
        return notesList
    }

    fun updateNote(note: Notes){
val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE,note.Title)
            put(COLUMN_CONTENT,note.Content)
        }

        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(note.id.toString())
        db.update(TABLE_NAME,values,whereClause,whereArgs)
        db.close()
    }

    fun getNoteByID(noteID:Int):Notes{
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_ID = $noteID"
        val cursor = db.rawQuery(query,null)
        cursor.moveToFirst()

        val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
        val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))

        cursor.close()
        db.close()
        return Notes(id,title,content)
    }
    fun deleteNote(noteID: Int) {

        val db = writableDatabase
        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(noteID.toString())
        db.delete(TABLE_NAME,whereClause,whereArgs)
        db.close()
    }
}