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
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor


class LoginActivity : AppCompatActivity() {
    private lateinit var bLogIn: Button
    private lateinit var vibrator: Vibrator
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var checkBox: CheckBox

    var mLoadingBar: ProgressDialog? = null

    private val executor: Executor? = null
    private val biometricPrompt: BiometricPrompt? = null
    //private val promptInfo: BiometricPrompt.PromptInfo? = null
    var mAuth: FirebaseAuth? = null

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