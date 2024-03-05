package com.example.notessqlite

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class NotesDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    //data storing and fetching is working fine.

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private val appContext: Context = context.applicationContext

    companion object {
        private const val DATABASE_NAME = "notesapp.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME_NOTES = "allnotes"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_SYNCED = "synced"  // Added column to track sync status
        private const val TABLE_NAME_USERS = "users"
        private const val COLUMN_EMAIL = "user_email"
        private const val COLUMN_NAME = "user_name"
    }


    override fun onCreate(db: SQLiteDatabase?) {
        val createNotesTableQuery =
            "CREATE TABLE $TABLE_NAME_NOTES($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_TITLE TEXT, $COLUMN_CONTENT TEXT, $COLUMN_SYNCED INTEGER)"
        db?.execSQL(createNotesTableQuery)

        val createUsersTableQuery =
            "CREATE TABLE $TABLE_NAME_USERS($COLUMN_ID INTEGER PRIMARY KEY,$COLUMN_NAME TEXT,$COLUMN_EMAIL TEXT,$COLUMN_SYNCED INTEGER)"
        db?.execSQL(createUsersTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val dropNotesTableQuery = "DROP TABLE IF EXISTS $TABLE_NAME_NOTES"
        db?.execSQL(dropNotesTableQuery)

        val dropUsersTableQuery = "DROP TABLE IF EXISTS $TABLE_NAME_USERS"
        db?.execSQL(dropUsersTableQuery)

        onCreate(db)
    }

    // Rest of the code remains the same

    fun insertNote(note: Notes) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, note.Title)
            put(COLUMN_CONTENT, note.Content)
            put(COLUMN_SYNCED, 0)  // Mark the note as not synced initially
        }

        val noteID = db.insert(TABLE_NAME_NOTES, null, values)
        db.close()

        // Queue the local change for offline sync
        if (noteID != -1L) {
            queueLocalChange(noteID.toString(), note)
        }

        // Check network availability
        if (isNetworkAvailable()) {
            // Sync local changes with Firestore
            syncLocalChangesWithFirestore()
            Log.d("Test", "syncLocalChange with firestore is called, networkAvailable is true")
        }
    }

    private fun queueLocalChange(noteID: String, note: Notes) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SYNCED, 0)  // Mark the note as not synced
        }

        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(noteID)
        db.update(TABLE_NAME_NOTES, values, whereClause, whereArgs)
        db.close()
    }

    private fun syncLocalChangesWithFirestore() {
        val localChanges = getUnsyncedNotes()

        currentUser?.let { user ->
            localChanges.forEach { (noteID, note) ->


                val userEmail = user.email ?: return@forEach // Return if user email is null

                val notesCollection =
                    firestore.collection("Users").document(userEmail).collection("notes")
                val noteDocument = notesCollection.document(noteID)

                val noteData = hashMapOf(
                    "title" to note.Title,
                    "content" to note.Content,
                    "id" to note.id
                )

                noteDocument.set(noteData)
                    .addOnSuccessListener {
                        // Successfully synced with Firestore
                        Log.d("Test", "sync successful")
                        markNoteAsSynced(noteID)
                    }
                    .addOnFailureListener { e ->
                        Log.d("NotesDatabaseHelper", "Error syncing with Firestore: ${e.message}")
                        // Handle failure to sync with Firestore
                    }
            }
        }
    }

    private fun markNoteAsSynced(noteID: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SYNCED, 1)  // Mark the note as synced
        }

        val whereClause = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(noteID)
        db.update(TABLE_NAME_NOTES, values, whereClause, whereArgs)
        db.close()

        Log.d("Test", "marked as sync")
    }

    private fun getUnsyncedNotes(): Map<String, Notes> {
        val db = readableDatabase
        val localChanges = mutableMapOf<String, Notes>()

        val query = "SELECT * FROM $TABLE_NAME_NOTES WHERE $COLUMN_SYNCED = 0"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
            val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))

            val note = Notes(id.toInt(), title, content)
            localChanges[id] = note

            Log.d("unsyncedNote", "$localChanges")

        }

        cursor.close()
        db.close()

        return localChanges
        Log.d("Test", "unsynced note fetched")

    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))

    }

    fun getAllNotes(): List<Notes> {
        val notesList = mutableListOf<Notes>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME_NOTES"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))

            val note = Notes(id, title, content)
            notesList.add(note)
        }

        cursor.close()
        db.close()

        return notesList
    }

    fun updateNote(note:Notes){
        val db=writableDatabase
        val values=ContentValues().apply{
            put(COLUMN_TITLE,note.Title)
            put(COLUMN_CONTENT,note.Content)
            put(COLUMN_SYNCED,0)
        }

        val whereClause="$COLUMN_ID=?"
        val whereArgs=arrayOf(note.id.toString())
        db.update(TABLE_NAME_NOTES,values,whereClause,whereArgs)
        db.close()
    }

    fun getNoteByID(noteID:Int):Notes{
        val db=readableDatabase
        val query="SELECT * FROM $TABLE_NAME_NOTES WHERE $COLUMN_ID=$noteID"
        val cursor=db.rawQuery(query,null)
        cursor.moveToFirst()

        val id=cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
        val  content=cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
        val title=cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))

        cursor.close()
        db.close()
        return Notes(id,title,content)
    }

    fun deleteNote(noteID:Int){

        val db=writableDatabase
        val whereClause="$COLUMN_ID=?"
        val whereArgs=arrayOf(noteID.toString())
        db.delete(TABLE_NAME_NOTES,whereClause,whereArgs)
        db.close()

        // Check network availability
        if (isNetworkAvailable()) {
            // If network is available, delete the note from Firestore
            deleteNoteFromFirestore(noteID)
            Log.d("Test", "deleteNoteFrom Firestore is called, network Available is true")
        }
    }

    private fun deleteNoteFromFirestore(noteID: Int) {
        currentUser?.let { user ->
            val userEmail = user.email ?: return
            val notesCollection =
                firestore.collection("Users").document(userEmail).collection("notes")
            val noteDocument = notesCollection.document(noteID.toString())

            noteDocument.delete()
                .addOnSuccessListener {
                    // Successfully deleted from Firestore
                    Log.d("Test", "Note deleted from Firestore")
                }
                .addOnFailureListener { e ->
                    Log.d("NotesDatabaseHelper", "Error deleting note from Firestore: ${e.message}")
                    // Handle failure to delete from Firestore
                }
        }
    }

    fun insertUser(user: User) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, user.userName)
            put(COLUMN_EMAIL, user.userEmail)
            put(COLUMN_SYNCED, 0)
        }

        db.insert(TABLE_NAME_USERS, null, values)
        db.close()
    }


    fun syncdatafromfirebase() {
        currentUser?.let { user ->
            val userEmail = user.email ?: return
            val usersCollection = firestore.collection("Users")
            val userDocument = usersCollection.document(userEmail)

            userDocument.get()
                .addOnSuccessListener { userSnapshot ->
                    val syncStatus = userSnapshot.getLong(COLUMN_SYNCED)
                    if (syncStatus == 0L) {
                        // Sync only if the sync status is 0
                        updateSyncStatus(userEmail)

                        
                    }
                }
                .addOnFailureListener { e ->
                    Log.d("NotesDatabaseHelper", "Error fetching user data from Firestore: ${e.message}")
                }
        }
    }

    fun updateSyncStatus(userEmail: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SYNCED, 1)
        }

        val whereClause = "$COLUMN_EMAIL = ?"
        val whereArgs = arrayOf(userEmail)
        db.update(TABLE_NAME_USERS, values, whereClause, whereArgs)
        db.close()

        Log.d("Test", "Sync status updated in local storage for user")
    }





    private fun insertNotetoLocalDb(notesList: List<Notes>) {
        val db = writableDatabase
        for (note in notesList) {
            val values = ContentValues().apply {
                put(COLUMN_TITLE, note.Title)
                put(COLUMN_CONTENT, note.Content)
                put(COLUMN_SYNCED, 1)  // Mark the note as synced
            }
            db.insert(TABLE_NAME_NOTES, null, values)
        }
        db.close()

        Log.d("Test", "Data synced from Firestore to local storage")
    }

}
