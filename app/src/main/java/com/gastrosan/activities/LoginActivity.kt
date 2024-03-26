package com.gastrosan.activities

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Bundle
import android.os.Vibrator
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.gastrosan.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.Executor


class LoginActivity : AppCompatActivity() {
    private lateinit var bLogIn: Button
    private lateinit var vibrator: Vibrator
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var checkBox: CheckBox

    private lateinit var bGoogle: Button

    var mLoadingBar: ProgressDialog? = null

    private val executor: Executor? = null
    private val biometricPrompt: BiometricPrompt? = null
    //private val promptInfo: BiometricPrompt.PromptInfo? = null
    var mAuth: FirebaseAuth? = null
    var database: FirebaseDatabase? = null

    lateinit var mGoogleSignInClient: GoogleSignInClient

    val RC_SIGN_IN = 20



    @SuppressLint("MissingInflatedId", "WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_login)

        bLogIn = findViewById(R.id.button1)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        checkBox = findViewById(R.id.checkBox)

        checkBox.setOnClickListener(View.OnClickListener {
           if(checkBox.isChecked){
               inputPassword.inputType = 1
        }else{
               inputPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        })

        bGoogle = findViewById(R.id.button2)


        bLogIn.setOnClickListener(View.OnClickListener {
            vibrator.vibrate(100)
            if (checkCredentials()) {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            }
        })
        inputEmail = findViewById(R.id.editText1)
        inputPassword = findViewById<EditText>(R.id.editText2)
        mAuth = FirebaseAuth.getInstance()
        mLoadingBar = ProgressDialog(this@LoginActivity)
        database = FirebaseDatabase.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
//            .requestProfile()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        bGoogle.setOnClickListener {
            googleSingIn()
        }
    }

    private fun googleSingIn() {
        val intent = mGoogleSignInClient.signInIntent
        startActivityForResult(intent, RC_SIGN_IN)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth?.signInWithCredential(credential)?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = mAuth?.currentUser
                val map = HashMap<String, Any>()
                map["id"] = user!!.uid
                map["name"] = user.displayName ?: ""
                //map.put("profile",user.photoUrl.toString())

                database?.reference?.child("Users")?.child(user.uid)?.setValue(map)

                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this@LoginActivity, "Algo fue mal", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { firebaseAuthWithGoogle(it) }
            } catch (e: ApiException) {
                Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun checkCredentials(): Boolean {
        val result = false
        val email: String = inputEmail.text.toString()
        val password: String = inputPassword.text.toString()

        return if (email.isEmpty() || !email.contains("@")) {
            showError(inputEmail, getString(R.string.email_error_message))
            result
        } else if (password.isEmpty() || password.length < 7) {
            showError(inputPassword, getString(R.string.passwd_error_message))
            result
        } else {
            mLoadingBar?.setTitle("Inicio de sesión")
            mLoadingBar?.setMessage(getString(R.string.message_wait))
            mLoadingBar?.setCanceledOnTouchOutside(false)
            mLoadingBar?.show()
            mAuth!!.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    mLoadingBar!!.dismiss()
                    //this.result=true;
                    Toast.makeText(this@LoginActivity,R.string.loggeidIn_message,Toast.LENGTH_SHORT).show()
                    val intent: Intent = Intent(this@LoginActivity,MainActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    mLoadingBar!!.dismiss()
                    Toast.makeText(this@LoginActivity,R.string.wrong_credentials,Toast.LENGTH_SHORT).show()
                }
            }
            return result
        }
    }

    fun signIn(view: View?) {
        startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
    }
    fun mainMenu(view: View?) {
        if (checkCredentials()) {
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        }
    }

    private fun showError(input: EditText, s: String) {
        input.error = s
        input.requestFocus()
    }
}