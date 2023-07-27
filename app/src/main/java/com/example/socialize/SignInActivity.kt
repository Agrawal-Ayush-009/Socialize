package com.example.socialize

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.socialize.daos.UserDao
import com.example.socialize.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class SignInActivity : AppCompatActivity() {

    private lateinit var googleSignInClient : GoogleSignInClient
    private lateinit var auth : FirebaseAuth
    private val TAG = "SignInActivity Tag"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // [START config_signIn]
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        // [END config_signIn]


        // [START initialize_auth]
        // Initialize Firebase Auth
        auth = Firebase.auth
        // [END initialize_auth]



        findViewById<Button>(R.id.BtnGoogleSignIn).setOnClickListener{
            signIn()
        }

    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    // [START signIn]
    private fun signIn(){
        val signInIntent = googleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }
    // [END signIn]

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>){

        try {
            val account : GoogleSignInAccount = completedTask.getResult(ApiException :: class.java)!!

            Log.d(TAG,"FireBaseAuthWithGoogle: " + account.id)
            firebaseAuthWithGoogle(account.idToken!!)
        }catch (e: ApiException){
            // Google Sign In failed, update UI appropriately
            Log.w(TAG, "signInResult: failedCode = " + e.statusCode)
        }
    }


    //[START auth_with_google]
    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        findViewById<Button>(R.id.BtnGoogleSignIn).visibility = View.GONE
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
        findViewById<ImageView>(R.id.google_image).visibility = View.GONE

        GlobalScope.launch(Dispatchers.IO) {//Coroutines to do works on thread other than main thread
            val auth = auth.signInWithCredential(credential).await()
            val firebaseUser = auth.user
            withContext(Dispatchers.Main) {//To Get Back to the main Thread
                updateUI(firebaseUser)
            }
        }
    }


    //[Update UI]
    private fun updateUI(firebaseUser: FirebaseUser?) {

        if(firebaseUser != null){

            // Adding User using UserDao
            val user = User(firebaseUser.uid, firebaseUser.displayName.toString(), firebaseUser.photoUrl.toString())
            val userDao = UserDao()
            userDao.addUser(user)

            val intent = Intent(this, MainActivity :: class.java)
                startActivity(intent)
                finish()
        }else{
            findViewById<Button>(R.id.BtnGoogleSignIn).visibility = View.VISIBLE
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            findViewById<ImageView>(R.id.google_image).visibility = View.VISIBLE
        }
    }

}