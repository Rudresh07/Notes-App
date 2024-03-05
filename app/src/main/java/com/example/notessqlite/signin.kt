package com.example.notessqlite

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class signin : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 9001
        var isSignedIn: Boolean = false // Companion object to hold signed-in status
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser

        if (currentUser != null) {
            // The user is already signed in, navigate to MainActivity
            isSignedIn = true // Update the signed-in status
            navigateToMainActivity()
            return
        }

        val signInButton = findViewById<ImageView>(R.id.googleimage)
        signInButton.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Successfully signed in
                    val user = auth.currentUser
                    user?.email?.let {
                        fetchNotesFromFirebase(it) // Pass the user's email to fetch their notes
                    }
                    isSignedIn = true // Update the signed-in status
                    navigateToMainActivity()
                } else {
                    // Authentication failed
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fetchNotesFromFirebase(userEmail: String) {
        val notesCollection = firestore.collection("Users").document(userEmail).collection("notes")

        notesCollection.get()
            .addOnSuccessListener { querySnapshot ->
                val notesList = mutableListOf<Notes>()

                for (document in querySnapshot.documents) {
                    val title = document.getString("title") ?: ""
                    val content = document.getString("content") ?: ""

                    val note = Notes(0, title, content)
                    notesList.add(note)

                }

                storeNotesLocally(notesList)

            }
            .addOnFailureListener { e ->
                Log.d("NotesDatabaseHelper", "Error fetching data from Firestore: ${e.message}")
                Toast.makeText(this, "Error fetching data from Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun storeNotesLocally(notesList: List<Notes>) {
        val dbHelper = NotesDatabaseHelper(this)
        for (note in notesList) {
            dbHelper.insertNote(note)
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Finish the sign-in activity
    }
}
