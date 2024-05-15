package com.gastrosan.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.gastrosan.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {
    private lateinit var barNameTxtView: EditText
    private lateinit var big_bar_nameTxtView: TextView
    private lateinit var addressEditText: EditText
    private lateinit var emailTxtView: TextView
    private lateinit var phoneEditText: EditText
    private lateinit var emailImageView: ImageView
    private lateinit var userImageView: CircleImageView
    private lateinit var signOutTxtView: TextView
    private lateinit var currentPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private val TAG = this::class.java.name.toUpperCase()
    private var email: String? = null
    private var userid: String? = null
    private var isEditModeEnabled = false // Variable para rastrear si la edición está habilitada
    private lateinit var confirmButton: Button
    private var tempImageUri: Uri? = null
    private var photoFileUri: Uri? = null

    companion object {
        private const val CAMERA_REQUEST_CODE = 1002
        private const val PERMISSIONS_REQUEST_CODE = 1001
        private const val GALLERY_REQUEST_CODE = 1003
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Recibir datos de la pantalla de inicio de sesión
        val intent = intent
        email = intent.getStringExtra("email")
        println("Email en Perfil: $email")

        // Referenciar las vistas
        barNameTxtView = findViewById(R.id.bar_name)
        big_bar_nameTxtView = findViewById(R.id.big_bar_name)
        addressEditText = findViewById(R.id.address)
        emailTxtView = findViewById(R.id.email_textview)
        phoneEditText = findViewById(R.id.phone_textview)
        userImageView = findViewById(R.id.user_imageview)
        emailImageView = findViewById(R.id.email_imageview)
        phoneEditText = findViewById(R.id.phone_textview)
        confirmButton = findViewById(R.id.confirm_button)
        signOutTxtView = findViewById(R.id.textView4)
        currentPasswordEditText = findViewById(R.id.current_password)
        newPasswordEditText = findViewById(R.id.new_password)

        loadUserProfile()

        userImageView.setOnClickListener {
            if (isEditModeEnabled) {
                checkAndRequestPermissions()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Cargar los datos del perfil cuando se reanuda la actividad
        loadUserProfile()
    }

    private fun loadUserProfile() {
        // Referencia a la base de datos de usuarios
        val rootRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/")
        val usersRef = rootRef.getReference("users")

        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (userSnapshot in dataSnapshot.children) {
                        val username = userSnapshot.child("username").getValue(String::class.java) ?: ""
                        val uid = userSnapshot.child("uid").getValue(String::class.java) ?: ""
                        val phone = userSnapshot.child("phone").getValue(String::class.java) ?: ""
                        val address = userSnapshot.child("address").getValue(String::class.java) ?: ""
                        val profilePicUrl = userSnapshot.child("profilePic").getValue(String::class.java) ?: ""
                        val email = email ?: ""

                        // Asignar el valor del uid al userid
                        userid = uid

                        // Convertir el nombre de usuario a mayúsculas
                        val uppercaseUsername = username.uppercase(Locale.ROOT)

                        // Mostrar los datos en las vistas
                        barNameTxtView.text = Editable.Factory.getInstance().newEditable(username)
                        emailTxtView.text = email
                        big_bar_nameTxtView.text = uppercaseUsername
                        phoneEditText.text = Editable.Factory.getInstance().newEditable(phone)
                        addressEditText.text = Editable.Factory.getInstance().newEditable(address)

                        // Cargar la imagen de perfil si existe
                        if (profilePicUrl.isNotEmpty()) {
                            Glide.with(this@ProfileActivity)
                                .load(profilePicUrl)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .placeholder(R.drawable.boy_avatar)
                                .error(R.drawable.boy_avatar)
                                .into(userImageView)
                        } else {
                            userImageView.setImageResource(R.drawable.boy_avatar)
                        }
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
        signOutTxtView.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        currentPasswordEditText.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        newPasswordEditText.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE

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

    // Método para actualizar los datos del perfil y la contraseña en la base de datos
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

        // Si hay una nueva imagen, subirla y luego actualizar el perfil
        tempImageUri?.let { newImageUri ->
            uploadImageUriToFirebase(newImageUri) { imageUrl ->
                userRef.child("profilePic").setValue(imageUrl)
                Glide.with(this)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(userImageView)
            }
        }

        // Actualizar la contraseña si se han proporcionado ambas contraseñas
        val currentPassword = currentPasswordEditText.text.toString()
        val newPassword = newPasswordEditText.text.toString()

        if (currentPassword.isNotEmpty() && newPassword.isNotEmpty()) {
            val user = FirebaseAuth.getInstance().currentUser
            val credential = EmailAuthProvider.getCredential(user?.email!!, currentPassword)

            user.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                            finishUpdate()
                        } else {
                            Toast.makeText(this, "Error al actualizar la contraseña", Toast.LENGTH_SHORT).show()
                            highlightPasswordFields()
                        }
                    }
                } else {
                    Toast.makeText(this, "La contraseña actual es incorrecta", Toast.LENGTH_SHORT).show()
                    highlightPasswordFields()
                }
            }
        } else {
            // Si no se están actualizando las contraseñas, finalizar la actualización del perfil
            finishUpdate()
        }
    }

    // Método para finalizar la actualización del perfil
    private fun finishUpdate() {
        // Mostrar un mensaje de éxito
        Toast.makeText(this@ProfileActivity, "Perfil actualizado", Toast.LENGTH_SHORT).show()

        // Deshabilitar el botón Confirmar Cambios y cambiar la imagen del lápiz a gris
        isEditModeEnabled = false
        confirmButton.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        currentPasswordEditText.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        newPasswordEditText.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        signOutTxtView.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        (findViewById<View>(R.id.edit_profile_button) as ImageView).setImageResource(R.drawable.baseline_edit_24_gray)

        // Deshabilitar la edición de los campos según el estado actual
        barNameTxtView.isEnabled = false
        addressEditText.isEnabled = false
        phoneEditText.isEnabled = false

        // Ocultar campos de contraseña y botón de actualización
        currentPasswordEditText.visibility = View.GONE
        newPasswordEditText.visibility = View.GONE

        // Recargar los datos del perfil
        loadUserProfile()
    }

    // Método para resaltar los campos de contraseña en rojo en caso de error
    private fun highlightPasswordFields() {
        currentPasswordEditText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        newPasswordEditText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
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

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_MEDIA_IMAGES
        )

        val permissionsToRequest = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        } else {
            // Todos los permisos están concedidos, proceder a abrir la cámara o galería
            showImagePickDialog()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Todos los permisos han sido concedidos
                showImagePickDialog()
            } else {
                Toast.makeText(this, "Se requieren permisos para acceder a la cámara y almacenamiento.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showImagePickDialog() {
        val options = arrayOf("Cámara", "Archivos del dispositivo")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Seleccionar imagen desde")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
            }
        }
        builder.show()
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Asegúrate de que haya una aplicación de cámara para manejar el intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Crea el archivo donde irá la foto
                val photoURI: Uri? = createImageFile()
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                photoFileUri = photoURI  // Guarda la URI globalmente para usar después
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
            }
        }
    }

    private fun createImageFile(): Uri? {
        val fileName = "JPEG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            fileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        ).let { file ->
            FileProvider.getUriForFile(this, "com.gastrosan.fileprovider", file)
        }
    }

    private fun openGallery() {
        val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhotoIntent, GALLERY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    // La imagen está siendo guardada como un archivo, se debe obtener de nuevo el Uri aquí
                    photoFileUri?.let {
                        println("Camera URI: $it")
                        userImageView.setImageURI(it)
                        tempImageUri = it  // Guardar la URI para usar después
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        println("Gallery URI: $uri")
                        userImageView.setImageURI(uri)
                        tempImageUri = uri  // Guardar la URI para usar después
                    }
                }
            }
        }
    }

    private val storageInstance: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    private fun uploadImageUriToFirebase(imageUri: Uri, callback: (String) -> Unit) {
        println("Uploading URI to Firebase: $imageUri")
        val filePath = "profile_pics/${UUID.randomUUID()}.jpg"
        val storageRef = storageInstance.getReference(filePath)

        storageRef.putFile(imageUri).addOnSuccessListener {
            it.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                println("Firebase Uploaded Image URI: $uri")
                callback(uri.toString())
                // Pre-cargar la imagen para mejorar la respuesta en la interfaz
                preloadImage(uri.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar la imagen: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun preloadImage(url: String) {
        Glide.with(this)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .preload()
    }
}
