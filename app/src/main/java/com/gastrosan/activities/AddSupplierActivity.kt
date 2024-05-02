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
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.IOException
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.gastrosan.databinding.ActivityAddSupplierBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage


class AddSupplierActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddSupplierBinding
    private var imageUri: Uri? = null

    // Registro del ActivityResultLauncher para el resultado de la actividad de captura o selección de imagen
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Maneja el resultado de la imagen, actualiza la variable global imageUri
            imageUri = result.data?.data  // Actualiza la variable global, no una local
            // Usa directamente el imageUri que se configuró al tomar la foto
            imageUri?.let { uri ->
                println("URI de la imagen capturada: $uri")
                binding.imageProvider.setImageURI(uri)
            } ?: Toast.makeText(this, "Error al cargar la imagen capturada", Toast.LENGTH_SHORT).show()

        }
    }
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Se necesita permiso de la cámara.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSupplierBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Establece el OnClickListener para el proveedor de imágenes
        binding.imageProvider.setOnClickListener {
            // Abrir el diálogo de selección de fuente de imagen
            showImageSourceDialog()
        }
        binding.btnSaveSupplier.setOnClickListener {
            // Verifica si hay una imagen seleccionada, si no, guarda sin imagen
            imageUri?.let {
                uploadImageToFirebaseStorage(it)
            } ?: run {
                saveSupplierToDatabase()  // Guarda sin imagen si no hay ninguna seleccionada
            }
        }
    }

    private fun showImageSourceDialog() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            chooseImageSource()
        }
    }

    private fun chooseImageSource() {
        val intents = arrayListOf<Intent>()

        // Intent para la cámara
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intents.add(takePictureIntent)
        intents.add(pickPhotoIntent)

        val chooserIntent = Intent.createChooser(Intent(), "¿De donde quieres obtener la imagen?")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
        startForResult.launch(chooserIntent)
    }

    /*private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.also {
            startForResult.launch(takePictureIntent)
        }
    }*/
    private fun createImageFile(): File {
        // Crear un nombre de archivo de imagen único
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).also {
            // Log the path for debugging
            println("Archivo creado en ${it.absolutePath}")
        }
    }
    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Crea el archivo donde se guardará la foto
            val photoFile: File? = try {
                createImageFile().apply {
                    // Guarda la URI del archivo temporal para usar después de capturar la foto
                    imageUri = FileProvider.getUriForFile(
                        this@AddSupplierActivity,
                        "${applicationContext.packageName}.provider",
                        this
                    )
                    println("URI del archivo temporal: $imageUri")
                }
            } catch (ex: IOException) {
                Toast.makeText(this, "Error al crear el archivo de imagen: ${ex.message}", Toast.LENGTH_SHORT).show()
                null
            }
            // Si el archivo se creó exitosamente, continúa y captura la imagen
            photoFile?.also {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startForResult.launch(takePictureIntent)
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
            Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_LONG).show()
            saveSupplierToDatabase()  // Intenta guardar el proveedor sin la imagen si la subida falla

        }
    }
    private fun saveSupplierToDatabase(imageUrl: String? = null) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users")

        val providerName = formatName(binding.editProviderName.text.toString())
        /*val contactName = formatName(binding.editContactName.text.toString())
        val contactPhone = formatPhoneNumber(binding.editContactPhone.text.toString())*/

        if (providerName.isNotEmpty()) {
            val supplierId = "supplier_${System.currentTimeMillis()}"
            val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

            val supplierData = hashMapOf(
                "id" to supplierId,
                "name" to providerName,
                //"contactName" to contactName,
                //"contactPhone" to contactPhone,
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
                    Toast.makeText(this, "Proveedor guardado exitosamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar el proveedor: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "El nombre del proveedor es obligatorio", Toast.LENGTH_SHORT).show()
        }
    }
    private fun formatName(name: String): String {
        return name.toLowerCase().capitalize()
    }

    private fun formatPhoneNumber(phoneNumber: String): Any {
        val digitsOnly = phoneNumber.replace("\\D".toRegex(), "")
        return if (digitsOnly.length == 9) {
            "+34 ${digitsOnly.substring(0, 3)} ${digitsOnly.substring(3, 6)} ${digitsOnly.substring(6)}"
        } else {
            Toast.makeText(this, "Número de teléfono no válido", Toast.LENGTH_SHORT).show()
        }
    }


    /*private fun updateSupplierInDatabase(imageUrl: String) {
        // Asume que ya tienes los datos del proveedor y solo necesitas actualizar la URL del logo
        // Obtener el ID del usuario y actualizar el proveedor con la nueva URL de la imagen
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return
        val supplierId = "supplierId"

        val databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/suppliers/$supplierId")
        databaseRef.child("logoUrl").setValue(imageUrl).addOnSuccessListener {
            Toast.makeText(this, "Proveedor actualizado con éxito", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Error al actualizar proveedor: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }*/
    companion object {
        private const val REQUEST_TAKE_PHOTO = 1
    }

}
