package com.gastrosan.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.provider.MediaStore
import android.app.Activity.RESULT_OK
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.FileProvider
import java.io.IOException
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.gastrosan.R
import com.gastrosan.databinding.ActivityAddSupplierBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage


class AddSupplierActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddSupplierBinding
    private var imageUri: Uri? = null
    private var photoFileUri: Uri? = null

    companion object {
        private const val CAMERA_REQUEST_CODE = 1002
        private const val GALLERY_REQUEST_CODE = 1003
        private const val PERMISSIONS_REQUEST_CODE = 1001
    }

    // Registro del ActivityResultLauncher para el resultado de la actividad de captura o selección de imagen
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Maneja el resultado de la imagen, actualiza la variable global imageUri
            imageUri = result.data?.data  // Actualiza la variable global, no una local
            // Usa directamente el imageUri que se configuró al tomar la foto
            imageUri?.let { uri ->
                println("URI de la imagen capturada: $uri")
                binding.imageProvider.setImageURI(uri)
            } ?: Toast.makeText(this,
                getString(R.string.error_al_cargar_la_imagen_capturada), Toast.LENGTH_SHORT).show()

        }
    }
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this,
                getString(R.string.se_necesita_permiso_de_la_c_mara), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSupplierBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Establece el OnClickListener para el proveedor de imágenes
        binding.imageProvider.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnSaveSupplier.setOnClickListener {
            vibrateButton(this)
            setResult(RESULT_OK)
            // Verifica si hay una imagen seleccionada, si no, guarda sin imagen
            imageUri?.let {
                uploadImageToFirebaseStorage(it)
            } ?: run {
                saveSupplierToDatabase()  // Guarda sin imagen si no hay ninguna seleccionada
            }
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

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES
        )

        val permissionsToRequest = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        } else {
            showImagePickDialog()
        }
    }
    private fun showImagePickDialog() {
        val options = arrayOf(getString(R.string.c_mara2),
            getString(R.string.archivos_del_dispositivo))
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.seleccionar_imagen_desde))
        builder.setItems(options) { _, which ->
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
                        binding.imageProvider.setImageURI(it)
                        imageUri = it  // Guardar la URI para usar después
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        println("Gallery URI: $uri")
                        binding.imageProvider.setImageURI(uri)
                        imageUri = uri  // Guardar la URI para usar después
                    }
                }
            }
        }
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return  // Asegúrate de que el usuario está autenticado antes de continuar

        // Extraer solo el nombre del archivo del URI, ignorando cualquier otro path
        val fileName = imageUri.lastPathSegment?.substringAfterLast('/') ?: "default_${System.currentTimeMillis()}.jpg"
        val storageRef = FirebaseStorage.getInstance().reference.child("suppliers_logo/${userId}/$fileName}")
        val uploadTask = storageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                saveSupplierToDatabase(imageUrl)  // Ahora pasamos la URL de la imagen a este método
            }
        }.addOnFailureListener {
            Toast.makeText(this, getString(R.string.upload_failed, it.message), Toast.LENGTH_LONG).show()
            saveSupplierToDatabase()  // Intenta guardar el proveedor sin la imagen si la subida falla
        }
    }

    private fun saveSupplierToDatabase(imageUrl: String? = null) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users")

        val providerName = formatName(binding.editProviderName.text.toString())

        if (providerName.isNotEmpty()) {
            val supplierId = "supplier_${System.currentTimeMillis()}"
            val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

            val supplierData = hashMapOf(
                "id" to supplierId,
                "name" to providerName,
                "registrationDate" to currentDate
            )
            imageUrl?.let { supplierData["logoUrl"] = it }
            val contactName = formatName(binding.editContactName.text.toString())
            if (contactName.isNotBlank()) supplierData["contactName"] = contactName
            val contactPhone = binding.editContactPhone.text.toString().let {
                if (it.matches("\\d{9}".toRegex())) "+34 $it" else null
            }
            contactPhone?.let { supplierData["contactPhone"] = it }

            databaseRef.child(userId).child("suppliers").child(supplierId)
                .setValue(supplierData)
                .addOnSuccessListener {
                    Toast.makeText(this,
                        getString(R.string.proveedor_guardado_exitosamente), Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this,
                        getString(R.string.error_al_guardar_el_proveedor, e.message), Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this,
                getString(R.string.el_nombre_del_proveedor_es_obligatorio), Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatName(name: String): String {
        return name.toLowerCase().capitalize()
    }


}
