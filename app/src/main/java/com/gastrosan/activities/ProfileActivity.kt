package com.gastrosan.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.*
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
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.os.Vibrator
import android.os.VibrationEffect
import android.text.InputType


class ProfileActivity : AppCompatActivity() {
    private lateinit var barNameTxtView: TextView
    private lateinit var barNameEdit: EditText
    private lateinit var bigBarNameTxtView: TextView
    private lateinit var addressTextView: TextView
    private lateinit var addressEdit: EditText
    private lateinit var emailTxtView: TextView
    private lateinit var emailTxtViewEdit: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var phoneEdit: EditText
    private lateinit var emailImageView: ImageView
    private lateinit var userImageView: CircleImageView
    private lateinit var signOutTxtView: TextView
    private lateinit var currentPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var logoImgView: ImageView
    private val TAG = this::class.java.name.toUpperCase()
    private var email: String? = null
    private var userid: String? = null
    private var isEditModeEnabled = false // Variable para rastrear si la edición está habilitada
    private lateinit var confirmButton: Button
    private var tempImageUri: Uri? = null
    private var photoFileUri: Uri? = null

    private lateinit var linearLayout: LinearLayout
    private lateinit var linearLayout4: LinearLayout
    private lateinit var linearLayoutPhone: LinearLayout
    private lateinit var linearLayoutPhoneEdit: LinearLayout
    private lateinit var linearLayoutCurrentPassword: LinearLayout
    private lateinit var linearLayoutNewPassword: LinearLayout
    private lateinit var showPasswordCheckBox: CheckBox

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
        barNameTxtView = findViewById(R.id.bar_name_text)
        barNameEdit = findViewById(R.id.bar_name_edit)
        bigBarNameTxtView = findViewById(R.id.big_bar_name)
        addressTextView = findViewById(R.id.address_text)
        addressEdit = findViewById(R.id.address_edit)
        emailTxtView = findViewById(R.id.email_textview)
        emailTxtViewEdit = findViewById(R.id.email_textview_edit)
        phoneTextView = findViewById(R.id.phone_text)
        phoneEdit = findViewById(R.id.phone_edit)
        userImageView = findViewById(R.id.user_imageview)
        emailImageView = findViewById(R.id.email_imageview)
        confirmButton = findViewById(R.id.confirm_button)
        signOutTxtView = findViewById(R.id.textView4)
        currentPasswordEditText = findViewById(R.id.current_password)
        newPasswordEditText = findViewById(R.id.new_password)
        linearLayout = findViewById(R.id.linearLayout)
        linearLayout4 = findViewById(R.id.linearLayout4)
        linearLayoutPhone = findViewById(R.id.linearLayout_phone)
        linearLayoutPhoneEdit = findViewById(R.id.linearLayout_phone_edit)
        linearLayoutCurrentPassword = findViewById(R.id.linearLayoutCurrentPassword)
        linearLayoutNewPassword = findViewById(R.id.linearLayoutNewPassword)
        showPasswordCheckBox = findViewById(R.id.show_password_checkbox)
        logoImgView = findViewById(R.id.logoImageView)

        loadUserProfile()

        userImageView.setOnClickListener {
            if (isEditModeEnabled) {
                checkAndRequestPermissions()
            }
        }
        showPasswordCheckBox.setOnCheckedChangeListener { _, isChecked ->
            togglePasswordVisibility(isChecked)
        }
    }

    override fun onResume() {
        super.onResume()
        // Cargar los datos del perfil cuando se reanuda la actividad
        loadUserProfile()
    }
    private fun togglePasswordVisibility(isChecked: Boolean) {
        val currentPasswordTypeface = currentPasswordEditText.typeface
        val currentPasswordTextSize = currentPasswordEditText.textSize

        val newPasswordTypeface = newPasswordEditText.typeface
        val newPasswordTextSize = newPasswordEditText.textSize

        if (isChecked) {
            currentPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            newPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            currentPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            newPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        currentPasswordEditText.typeface = currentPasswordTypeface
        currentPasswordEditText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, currentPasswordTextSize)

        newPasswordEditText.typeface = newPasswordTypeface
        newPasswordEditText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newPasswordTextSize)

        // Mover el cursor al final del texto
        currentPasswordEditText.setSelection(currentPasswordEditText.text.length)
        newPasswordEditText.setSelection(newPasswordEditText.text.length)
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
                        barNameTxtView.text = username
                        barNameEdit.text = Editable.Factory.getInstance().newEditable(username)
                        emailTxtView.text = email
                        emailTxtViewEdit.text = email
                        bigBarNameTxtView.text = uppercaseUsername
                        phoneTextView.text = phone
                        phoneEdit.text = Editable.Factory.getInstance().newEditable(phone)
                        addressTextView.text = address
                        addressEdit.text = Editable.Factory.getInstance().newEditable(address)

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

        // Alternar visibilidad entre EditText y TextView
        barNameTxtView.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        barNameEdit.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        addressTextView.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        addressEdit.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        phoneTextView.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        phoneEdit.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        linearLayout.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        linearLayout4.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        linearLayoutPhone.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        linearLayoutPhoneEdit.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        linearLayoutCurrentPassword.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        linearLayoutNewPassword.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        logoImgView.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE

        // Cambiar la visibilidad del botón
        confirmButton.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        showPasswordCheckBox.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
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
    }



    // Método para actualizar los datos del perfil y la contraseña en la base de datos
    fun updateUserProfile(view: View) {
        vibrateButton(this) // Llamar a la función de vibración

        // Obtener los valores actuales
        val currentUsername = barNameEdit.text.toString()
        val currentAddress = addressEdit.text.toString()
        val currentPhone = phoneEdit.text.toString()
        // Obtener las contraseñas
        val currentPassword = currentPasswordEditText.text.toString()
        val newPassword = newPasswordEditText.text.toString()


        // Verificar si hay cambios
        val isUsernameChanged = currentUsername != barNameTxtView.text.toString()
        val isAddressChanged = currentAddress != addressTextView.text.toString()
        val isPhoneChanged = currentPhone != phoneTextView.text.toString()
        val isProfilePicChanged = tempImageUri != null
        val isPasswordChanged = currentPassword.isNotEmpty() && newPassword.isNotEmpty()


        if (!isUsernameChanged && !isAddressChanged && !isPhoneChanged && !isProfilePicChanged && !isPasswordChanged) {
            vibrateButton(this)
            Toast.makeText(this, "No hay ningún cambio realizado", Toast.LENGTH_SHORT).show()
            return
        }

        // Actualizar los valores en la base de datos
        val usersRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users")
        val userRef = usersRef.child(userid.toString())
        userRef.child("username").setValue(currentUsername)
        userRef.child("address").setValue(currentAddress)
        userRef.child("phone").setValue(currentPhone)

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
    fun vibrateButton(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator == null) {
            println("Servicio de vibración no disponible")
            return
        }
        if (!vibrator.hasVibrator()) {
            println("El dispositivo no tiene vibrador")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Para dispositivos con API 26 o superior
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            println("Vibrando API 26+")
        } else {
            // Para dispositivos con API menor a 26
            vibrator.vibrate(100)
            println("Vibrando API menor a 26")
        }
    }


    // Método para finalizar la actualización del perfil
    private fun finishUpdate() {
        // Mostrar un mensaje de éxito
        Toast.makeText(this@ProfileActivity, "El Perfil ha sido Actualizado", Toast.LENGTH_SHORT).show()

        // Deshabilitar el botón Confirmar Cambios y cambiar la imagen del lápiz a gris
        isEditModeEnabled = false
        confirmButton.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        currentPasswordEditText.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        newPasswordEditText.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        signOutTxtView.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        showPasswordCheckBox.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        (findViewById<View>(R.id.edit_profile_button) as ImageView).setImageResource(R.drawable.baseline_edit_24_gray)

        // Alternar visibilidad entre EditText y TextView
        barNameTxtView.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        barNameEdit.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        addressTextView.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        addressEdit.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        phoneTextView.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        phoneEdit.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        linearLayoutPhone.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        linearLayoutPhoneEdit.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        linearLayout4.visibility = if (isEditModeEnabled) View.VISIBLE else View.GONE
        linearLayout.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        logoImgView.visibility = if (isEditModeEnabled) View.GONE else View.VISIBLE
        linearLayoutCurrentPassword.visibility = View.GONE
        linearLayoutNewPassword.visibility = View.GONE

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
        builder.setTitle("Estás a punto de cerrar sesión")
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
