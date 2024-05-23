package com.gastrosan.activities

import Users
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gastrosan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest

class RegisterActivity : AppCompatActivity() {

    lateinit var mDatabase: DatabaseReference

    var inputUserName: EditText? = null
    var inputPassword: EditText? = null
    var inputEmail: EditText? = null
    var bSignIn: Button? = null
    var vibrator: Vibrator? = null
    var mAuth: FirebaseAuth? = null
    private var mLoadingBar: ProgressDialog? = null
    var vibratorS: Vibrator? = null
    var sharePreferences: SharedPreferences? = null
    var editor: SharedPreferences.Editor? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mDatabase = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users")

        inputUserName = findViewById(R.id.username)
        inputEmail = findViewById(R.id.email)
        inputPassword = findViewById(R.id.passwd)
        bSignIn = findViewById(R.id.button1)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        mAuth = FirebaseAuth.getInstance()
        mLoadingBar = ProgressDialog(this@RegisterActivity)

        sharePreferences = getSharedPreferences("MODE", MODE_PRIVATE)
        vibratorS = getSystemService(VIBRATOR_SERVICE) as Vibrator

        bSignIn?.setOnClickListener(View.OnClickListener {
            vibrateButton(this@RegisterActivity)
            if (checkCredentials()) {
                // Mover el inicio de LoginActivity aquí solo si la creación de usuario y la escritura en la base de datos son exitosas.
            }
        })
    }

    fun vibrateButton(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Para dispositivos con API 26 o superior
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // Para dispositivos con API menor a 26
            vibrator.vibrate(100)
        }
    }

    fun checkCredentials(): Boolean {
        val username = inputUserName!!.text.toString()
        val email = inputEmail!!.text.toString()
        val password = inputPassword!!.text.toString()
        if (username.isEmpty()) {
            showError(inputUserName!!, getString(R.string.username_error))
            return false
        } else if (email.isEmpty() || !email.contains("@")) {
            showError(inputEmail!!, getString(R.string.email_error_message))
            return false
        } else if (password.isEmpty() || password.length < 7) {
            showError(inputPassword!!, getString(R.string.passwd_error_message))
            return false
        } else {
            mLoadingBar!!.setTitle(getString(R.string.registration))
            mLoadingBar!!.setMessage(getString(R.string.message_wait))
            mLoadingBar!!.setCanceledOnTouchOutside(false)
            mLoadingBar!!.show()
            mAuth!!.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("Usuario creado con éxito")
                    writeNewUser()
                } else {
                    mLoadingBar!!.dismiss()
                    Toast.makeText(
                        this@RegisterActivity,
                        task.exception?.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        return true
    }

    fun writeNewUser() {
        val password = inputPassword!!.text.toString()
        val hashedPassword = hashPassword(password)
        val uid = mAuth?.currentUser?.uid ?: ""
        val user = Users(
            uid,
            inputUserName!!.text.toString(),
            inputEmail!!.text.toString(),
            hashedPassword,
            "",
            "",
            "",
            ""  // Initializing 'suppliers' as an empty string or map if necessary
        )

        mDatabase.child(uid).setValue(user).addOnCompleteListener { task ->
            mLoadingBar!!.dismiss()
            if (task.isSuccessful) {
                Toast.makeText(
                    this@RegisterActivity,
                    R.string.account_created,
                    Toast.LENGTH_SHORT
                ).show()
                // Iniciar la LoginActivity solo si la escritura en la base de datos es exitosa
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } else {
                Log.e("RegisterActivity", "Error al guardar el usuario en la base de datos: ${task.exception?.message}")
            }
        }
        Log.d("RegisterActivity", "Nuevo usuario guardado en la base de datos: $user")
    }

    private fun showError(input: EditText, s: String) {
        input.error = s
        input.requestFocus()
    }

    fun logIn(view: View?) {
        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
    }

    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(bytes)
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
}
