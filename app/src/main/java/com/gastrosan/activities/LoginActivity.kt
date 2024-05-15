package com.gastrosan.activities

import Users
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Bundle
import android.os.Vibrator
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.MessageDigest
import java.util.concurrent.Executor


class LoginActivity : AppCompatActivity() {
    private lateinit var bLogIn: Button
    private lateinit var vibrator: Vibrator
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var checkBox: CheckBox
    private lateinit var bGoogle: ImageView
    var mLoadingBar: ProgressDialog? = null

    var mAuth: FirebaseAuth? = null
    var database: FirebaseDatabase? = null
    lateinit var mGoogleSignInClient: GoogleSignInClient
    val RC_SIGN_IN = 20

    @SuppressLint("MissingInflatedId", "WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
        // Chequear si el usuario está ya logueado
        if (mAuth!!.currentUser != null) {
            // El usuario está logueado, redirigir al MenuActivity
            goToMenuActivity()
        } else {
            // El usuario no está logueado, inicializar componentes para login
            initializeLoginComponents()
        }

    }
    private fun initializeLoginComponents() {
        bLogIn = findViewById(R.id.button1)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        inputEmail = findViewById(R.id.editText1)
        inputPassword = findViewById(R.id.editText2)
        checkBox = findViewById(R.id.checkBox)
        bGoogle = findViewById(R.id.button2)
        mLoadingBar = ProgressDialog(this)

        // Inicializar el ProgressDialog con la configuración adecuada
        mLoadingBar = ProgressDialog(this).apply {
            setTitle("Inicio de sesión")
            setMessage("Por favor, espere mientras verificamos sus credenciales...")
            setCanceledOnTouchOutside(false)
        }
        setupSignInMethods()

    }
    private fun setupSignInMethods() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        bLogIn.setOnClickListener {
            performLogin()
        }

        bGoogle.setOnClickListener {
            googleSignIn()
        }

        checkBox.setOnClickListener {
            if (checkBox.isChecked) {
                inputPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                inputPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        }
    }
    private fun performLogin() {
        val email = inputEmail.text.toString()
        val password = inputPassword.text.toString()

        if (email.isEmpty() || !email.contains("@")) {
            showError(inputEmail, "Por favor, introduzca un correo electrónico válido.")
            return
        } else if (password.isEmpty() || password.length < 6) {
            showError(inputPassword, "La contraseña debe tener al menos 6 caracteres.")
            return
        }

        mLoadingBar?.show()

        mAuth?.signInWithEmailAndPassword(email, password)?.addOnCompleteListener { task ->
            mLoadingBar?.dismiss()
            if (task.isSuccessful) {
                goToMenuActivity()
            } else {
                Toast.makeText(this, "Email o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun googleSignIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth?.signInWithCredential(credential)?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = mAuth?.currentUser
                user?.let {
                    checkAndSaveUser(it)
                }
                goToMenuActivity()
            } else {
                Toast.makeText(this, "Ha ocurrido un error en nuestros servidores", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun checkAndSaveUser(user: FirebaseUser) {
        val database = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users")
        val uid = user.uid

        database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val email = user.email
                    val username = user.displayName ?: email?.substringBefore("@")
                    val profilePic = user.photoUrl?.toString() ?: ""

                    val newUser = Users(
                        uid = uid,
                        username = username,
                        email = email,
                        password = null, // Contraseña no se almacena para usuarios de Google
                        phone = null,
                        address = null,
                        profilePic = profilePic
                    )

                    database.child(uid).setValue(newUser).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("LoginActivity", "Usuario guardado en la base de datos: $newUser")
                        } else {
                            Log.e("LoginActivity", "Error al guardar el usuario en la base de datos", task.exception)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoginActivity", "Error al verificar la existencia del usuario", error.toException())
            }
        })
    }

    private fun goToMenuActivity() {
        startActivity(Intent(this, MenuActivity::class.java))
        finish()
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
        val hashedPassword = hashPassword(password) // Hashea la contraseña ingresada antes de la comparación

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

                    // Obtener el correo electrónico del usuario actual
                    val user = FirebaseAuth.getInstance().currentUser
                    val email = user?.email
                    println("Email en Login: $email")

                    val intent: Intent = Intent(this@LoginActivity,MenuActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra("email", email)
                    startActivity(intent)

                    // Finalizar esta actividad para que no se pueda volver atrás desde la actividad de perfil
                    finish()
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
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(bytes)
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }


    private fun showError(input: EditText, s: String) {
        input.error = s
        input.requestFocus()
    }
}