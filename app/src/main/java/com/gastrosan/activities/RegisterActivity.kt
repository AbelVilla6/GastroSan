package com.gastrosan.activities

import Users
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gastrosan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class RegisterActivity : AppCompatActivity() {
    val firebaseDatabase = FirebaseDatabase.getInstance()

    var inputUserName: EditText? = null
    var inputPassword:EditText? = null
    var inputEmail:EditText? = null
    var bSignIn: Button? = null
    var vibrator: Vibrator? = null
    private var mAuth: FirebaseAuth? = null
    private var mLoadingBar: ProgressDialog? = null
    var vibratorS: Vibrator? = null
    var sharePreferences: SharedPreferences? = null
    var editor: SharedPreferences.Editor? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

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
            vibrator!!.vibrate(100)
            if (checkCredentials()) {
                Toast.makeText(
                    this@RegisterActivity,
                    R.string.account_created,
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
            }
        })
    }

    private fun checkCredentials(): Boolean {
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
                    mLoadingBar!!.dismiss()
                    Toast.makeText(
                        this@RegisterActivity,
                        R.string.success_registration,
                        Toast.LENGTH_SHORT
                    ).show()
                    val uid = task.result.user!!.uid
                    val user = Users(
                        uid,
                        inputUserName!!.text.toString(),
                        inputEmail!!.text.toString(),
                        inputPassword!!.text.toString(),
                        0
                    )
                    firebaseDatabase.reference.child("Users").child(uid).setValue(user)
                    val intent: Intent = Intent(this@RegisterActivity,LoginActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this@RegisterActivity,
                        task.exception.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        return true
    }

    private fun showError(input: EditText, s: String) {
        input.error = s
        input.requestFocus()
    }
    fun logIn(view: View?) {
        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
    }

}