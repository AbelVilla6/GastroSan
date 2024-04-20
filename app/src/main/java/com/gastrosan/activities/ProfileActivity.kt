package com.gastrosan.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.gastrosan.R
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView;
import android.widget.Toast
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.*
import android.text.Editable
import androidx.appcompat.app.AlertDialog
import com.gastrosan.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.StorageReference
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber


class ProfileActivity : AppCompatActivity() {
    private lateinit var barNameTxtView: EditText
    private lateinit var big_bar_nameTxtView: TextView
    private lateinit var addressEditText: EditText
    private lateinit var emailTxtView: TextView
    private lateinit var phoneEditText: EditText
    private lateinit var emailImageView: ImageView
    private lateinit var userImageView: CircleImageView
    private val TAG = this::class.java.name.toUpperCase()
    private var email: String? = null
    private var userid: String? = null
    private var isEditModeEnabled = false // Variable para rastrear si la edición está habilitada
    private lateinit var confirmButton: Button



    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        //receive data from login screen
        val intent = intent
        email = intent.getStringExtra("email")
        println("Email en Perfil: $email")

        //Referenciar las vistas
        barNameTxtView = findViewById(R.id.bar_name)
        big_bar_nameTxtView = findViewById(R.id.big_bar_name)
        addressEditText = findViewById(R.id.address)
        emailTxtView = findViewById(R.id.email_textview)
        phoneEditText = findViewById(R.id.phone_textview)
        userImageView = findViewById(R.id.user_imageview)
        emailImageView = findViewById(R.id.email_imageview)
        phoneEditText = findViewById(R.id.phone_textview)
        confirmButton = findViewById(R.id.confirm_button)

        loadUserProfile()

    }
    override fun onResume() {
        super.onResume()
        //load user profile data when activity is resumed
        loadUserProfile()
    }
    private fun loadUserProfile() {
        // Referencia a la base de datos de usuarios
        val rootRef =
            FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/")
        println("RootRef: $rootRef")
        val usersRef = rootRef.getReference("users")
        println("UsersRef: $usersRef")

        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (userSnapshot in dataSnapshot.children) {
                        val username = userSnapshot.child("username").getValue(String::class.java)
                        val uid = userSnapshot.child("uid").getValue(String::class.java)
                        val phone = userSnapshot.child("phone").getValue(String::class.java)
                        val address = userSnapshot.child("address").getValue(String::class.java)
                        val email = email
                        println("Username impreso: $username")
                        println("Email impreso: $email")

                        // Asignar el valor del uid al userid
                        userid = uid

                        // Convertir el nombre de usuario a mayúsculas
                        val uppercaseUsername = username?.uppercase(Locale.ROOT)

                        // Mostrar los datos en las vistas
                        barNameTxtView.text = Editable.Factory.getInstance().newEditable(username)
                        emailTxtView.text = email
                        big_bar_nameTxtView.text = uppercaseUsername
                        phoneEditText.text = Editable.Factory.getInstance().newEditable(phone)
                        addressEditText.text = Editable.Factory.getInstance().newEditable(address)

                    }
                } else {
                    Log.d(TAG, "No se encontró ningún usuario con el correo electrónico proporcionado")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    // Método para manejar el clic en el ImageView del lápiz
    fun editProfile(view: View) {
        isEditModeEnabled = !isEditModeEnabled // Alternar el estado de la edición

        // Habilitar o deshabilitar la edición de los campos según el estado actual
        barNameTxtView.isEnabled = isEditModeEnabled
        addressEditText.isEnabled = isEditModeEnabled
        phoneEditText.isEnabled = isEditModeEnabled

        // Cambiar la visibilidad del botón
        confirmButton.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE

        // Si se ha habilitado la edición, cambiar la imagen del lápiz a una marca de verificación
        if (isEditModeEnabled) {
            (view as ImageView).setImageResource(R.drawable.baseline_edit_24)
        } else {
            // Si se ha deshabilitado la edición, restaurar la imagen del lápiz
            (view as ImageView).setImageResource(R.drawable.baseline_edit_24_gray)
        }

        // Mostrar un mensaje de confirmación
        val message = if (isEditModeEnabled) "Editando perfil" else "Edición finalizada"
        Toast.makeText(this@ProfileActivity, message, Toast.LENGTH_SHORT).show()



    }

    // Método para actualizar los datos del perfil en la base de datos
    fun updateUserProfile(view: View) {
        val newUsername = barNameTxtView.text.toString()
        val newAddress = addressEditText.text.toString()
        val newPhone = phoneEditText.text.toString()


        // Actualizar los valores en la base de datos
        val usersRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users")
        val userRef = usersRef.child(userid.toString())
        userRef.child("username").setValue(newUsername)
        userRef.child("address").setValue(newAddress)
        userRef.child("phone").setValue(newPhone)

        // Mostrar un mensaje de éxito
        Toast.makeText(this@ProfileActivity, "Perfil actualizado", Toast.LENGTH_SHORT).show()

        // Deshabilitar el botón Confirmar Cambios y cambiar la imagen del lápiz a gris
        isEditModeEnabled = false
        confirmButton.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        (findViewById<View>(R.id.edit_profile_button) as ImageView).setImageResource(R.drawable.baseline_edit_24_gray)

        // Deshabilitar la edición de los campos según el estado actual
        barNameTxtView.isEnabled = false
        addressEditText.isEnabled = false
        phoneEditText.isEnabled = false

        // Recargar los datos del perfil
        loadUserProfile()
    }
    fun signOut(view: View) {
        // Mostrar el diálogo de confirmación
        showSignOutConfirmationDialog()
    }

    private fun showSignOutConfirmationDialog() {
        // Crear el diálogo de confirmación
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmar")
        builder.setMessage("¿Estás seguro de que quieres cerrar sesión?")

        // Botón "Sí": cierra sesión
        builder.setPositiveButton("Sí") { dialog, which ->
            // Cerrar sesión
            FirebaseAuth.getInstance().signOut()
            // Redirigir a la pantalla de inicio de sesión
            val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Botón "No": cancela
        builder.setNegativeButton("No") { dialog, which ->
            // Cerrar el diálogo sin hacer nada
            dialog.dismiss()
        }

        // Mostrar el diálogo
        val dialog = builder.create()
        dialog.show()
    }



}

