package com.gastrosan.activities

import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.gastrosan.R
import com.gastrosan.activities.ui.camera.CameraFragment
import com.gastrosan.activities.ui.dashboard.DashboardFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.database.DatabaseReference
import java.io.File
import java.io.OutputStream
import java.util.Date
import java.util.UUID


class SupplierActivity : AppCompatActivity() {
    private lateinit var addSupplier: ImageView
    private lateinit var deleteSupplier: ImageView
    private lateinit var gridView: GridView // Agregar esta línea
    private lateinit var buttonDelete: Button
    private lateinit var buttonCancel: Button
    private lateinit var buttonDeleteSupplier: Button
    private lateinit var editButton: ImageView
    private lateinit var buttonConfirmChanges: Button
    private lateinit var textViewSupplierName: TextView
    private lateinit var editTextSupplierName: EditText
    private lateinit var textViewContactName :  TextView
    private lateinit var editTextContactName : EditText
    private lateinit var textViewContactPhone: TextView
    private lateinit var editTextContactPhone: EditText
    private lateinit var imageViewLogo : ImageView

    private var originalSupplierName: String? = null
    private var originalContactName: String? = null
    private var originalContactPhone: String? = null
    private var originalLogoUri: Uri? = null

    private var cambiosConfirmados = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supplier)

        addSupplier = findViewById(R.id.addSupplier)
        deleteSupplier = findViewById(R.id.deleteSupplier)
        gridView = findViewById(R.id.gridViewInvoices)
        buttonDelete = findViewById(R.id.buttonDelete)
        buttonCancel = findViewById(R.id.buttonCancel)
        buttonDeleteSupplier = findViewById(R.id.buttonDeleteSupplier)
        editButton = findViewById(R.id.edit_supplier_button)
        buttonConfirmChanges = findViewById(R.id.buttonConfirmChanges)
        textViewSupplierName = findViewById(R.id.textViewSupplierName)
        editTextSupplierName = findViewById(R.id.editTextSupplierName)
        textViewContactName = findViewById(R.id.textViewContactName)
        editTextContactName = findViewById(R.id.editTextViewContactName)
        textViewContactPhone = findViewById(R.id.textViewContactPhone)
        editTextContactPhone = findViewById(R.id.editTextViewContactPhone)
        imageViewLogo = findViewById(R.id.imageViewLogo)

        // Recuperar el ID pasado desde DashboardFragment
        val supplierId = intent.getStringExtra("supplierId")
        if (supplierId != null) {
            loadSupplierDetails(supplierId)
        }

        addSupplier.setOnClickListener {
            val intent = Intent(this@SupplierActivity, MenuActivity::class.java)
            intent.putExtra("navigateToCameraFragment", true)
            startActivity(intent)
        }

        deleteSupplier.setOnClickListener {
            val adapter = gridView.adapter as InvoiceAdapter
            adapter.toggleSelectMode()
            addSupplier.visibility = View.GONE
            deleteSupplier.visibility = View.GONE
            buttonDelete.visibility = View.VISIBLE
            buttonCancel.visibility = View.VISIBLE
        }

        buttonDelete.setOnClickListener {
            val adapter = gridView.adapter as InvoiceAdapter
            val selectedItems = adapter.getSelectedItems()

            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "No hay facturas seleccionadas para eliminar", Toast.LENGTH_SHORT).show()
            } else {
                // Mostrar un diálogo de confirmación antes de eliminar
                AlertDialog.Builder(this)
                    .setTitle("Confirmar eliminación")
                    .setMessage("¿Estás seguro de que deseas eliminar las facturas seleccionadas?")
                    .setPositiveButton("Eliminar") { dialog, which ->
                        deleteInvoices(selectedItems, adapter)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }        }
        buttonCancel.setOnClickListener {
            val adapter = gridView.adapter as InvoiceAdapter
            adapter.toggleSelectMode()
            addSupplier.visibility = View.VISIBLE
            deleteSupplier.visibility = View.VISIBLE
            buttonDelete.visibility = View.GONE
            buttonCancel.visibility = View.GONE
        }
        buttonDeleteSupplier.setOnClickListener { showDeleteConfirmationDialog()}

        editButton.setOnClickListener {
            toggleEditMode()
        }
        buttonConfirmChanges.setOnClickListener {
            confirmChanges()
        }


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
    private var tempImageUri: Uri? = null  // Variable para guardar temporalmente la URI de la nueva imagen

    private var photoFileUri: Uri? = null  // Variable global para guardar la Uri del archivo de foto

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
                        imageViewLogo.setImageURI(it)
                        tempImageUri = it  // Guardar la URI para usar después
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        imageViewLogo.setImageURI(uri)
                        tempImageUri = uri  // Guardar la URI para usar después
                    }
                }
            }
        }
    }



    private val storageInstance: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
    private fun saveImageToExternalStorage(bitmap: Bitmap): Uri? {
        // Guardar el Bitmap en el almacenamiento externo
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        var imageUri: Uri? = null
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }

        // Obteniendo el URI de la imagen guardada
        try {
            contentResolver.also { resolver ->
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
            fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }

        return imageUri
    }


    private fun uploadImageUriToFirebase(imageUri: Uri, callback: (String) -> Unit) {
        val supplierId = intent.getStringExtra("supplierId") ?: return
        val filePath = "suppliers/$supplierId/logo/${UUID.randomUUID()}.jpg"
        val storageRef = storageInstance.getReference(filePath)

        storageRef.putFile(imageUri).addOnSuccessListener { taskSnapshot ->
            taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                callback(uri.toString())
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error al cargar la imagen: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateSupplierLogoUrl(supplierId: String, imageUrl: String) {
        val supplierRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("users/${FirebaseAuth.getInstance().currentUser?.uid}/suppliers/$supplierId")

        val updateMap = mapOf("logoUrl" to imageUrl)
        supplierRef.updateChildren(updateMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "URL del logo actualizado correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al actualizar el URL del logo", Toast.LENGTH_SHORT).show()
                }
            }
    }


    companion object {
        private const val CAMERA_REQUEST_CODE = 1002
        private const val PERMISSIONS_REQUEST_CODE = 1001
        private const val GALLERY_REQUEST_CODE = 1003
    }


    private fun confirmChanges() {
        val supplierId = intent.getStringExtra("supplierId") ?: return
        val newSupplierName = editTextSupplierName.text.toString()
        val newContactName = editTextContactName.text.toString()
        val newContactPhone = editTextContactPhone.text.toString()

        // Obtener referencia a la vista de animación
        val lottieAnimationView = findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        lottieAnimationView.visibility = View.VISIBLE
        lottieAnimationView.playAnimation()

        // Detener la animación después de un delay
        Handler(Looper.getMainLooper()).postDelayed({
            lottieAnimationView.visibility = View.GONE
            lottieAnimationView.cancelAnimation()
        }, 1000)  // Delay de 1000 ms (1 segundos)

        val supplierRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("users/${FirebaseAuth.getInstance().currentUser?.uid}/suppliers/$supplierId")

        val updateMap = mutableMapOf<String, Any>(
            "name" to newSupplierName,
            "contactName" to newContactName,
            "contactPhone" to newContactPhone
        )

        // Si hay una nueva imagen, subirla y actualizar el URL en la base de datos
        tempImageUri?.let {
            uploadImageUriToFirebase(it) { imageUrl ->
                updateMap["logoUrl"] = imageUrl
                updateSupplierDetails(supplierRef, updateMap)
            }
        } ?: updateSupplierDetails(supplierRef, updateMap)  // No hay nueva imagen, solo actualizar otros datos
    }

    private fun updateSupplierDetails(supplierRef: DatabaseReference, updateMap: Map<String, Any>) {
        supplierRef.updateChildren(updateMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show()
                tempImageUri = null  // Limpiar la URI temporal después de la actualización
                cambiosConfirmados = true  // Marcar que los cambios han sido confirmados
                toggleEditMode()  // Llamar a toggleEditMode para volver a modo de visualización
            } else {
                Toast.makeText(this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun toggleEditMode() {
        if (textViewSupplierName.visibility == View.VISIBLE) {
            // Entrar en modo de edición
            enterEditMode()
        } else {
            // Salir del modo de edición, pero no confirmar cambios aquí
            exitEditMode()
        }
    }

    private fun enterEditMode() {
        // Guarda los valores actuales antes de hacer cambios
        originalSupplierName = textViewSupplierName.text.toString()
        originalContactName = textViewContactName.text.toString()
        originalContactPhone = textViewContactPhone.text.toString()
        originalLogoUri = if (imageViewLogo.tag != null) imageViewLogo.tag as Uri else null
        println("Original logo URI: $originalLogoUri")

        textViewSupplierName.visibility = View.GONE
        editTextSupplierName.visibility = View.VISIBLE
        editTextSupplierName.setText(originalSupplierName)

        textViewContactName.visibility = View.GONE
        editTextContactName.visibility = View.VISIBLE
        editTextContactName.setText(originalContactName)

        textViewContactPhone.visibility = View.GONE
        editTextContactPhone.visibility = View.VISIBLE
        editTextContactPhone.setText(originalContactPhone)

        addSupplier.visibility = View.GONE
        deleteSupplier.visibility = View.GONE
        buttonDeleteSupplier.visibility = View.GONE

        imageViewLogo.setOnClickListener {
            checkAndRequestPermissions()
        }

        /*editTextSupplierName.setText(textViewSupplierName.text)
        editTextContactName.setText(textViewContactName.text)
        editTextContactPhone.setText(textViewContactPhone.text)*/

        buttonConfirmChanges.visibility = View.VISIBLE
        editButton.setImageResource(R.drawable.baseline_edit__blue_24)
    }

    private fun exitEditMode() {
        if(!cambiosConfirmados){
            // Restablecer los valores originales
            textViewSupplierName.text = originalSupplierName
            textViewContactName.text = originalContactName
            textViewContactPhone.text = originalContactPhone
        }else{
            // Restablecer los valores editados
            textViewSupplierName.text = editTextSupplierName.text
            textViewContactName.text = editTextContactName.text
            textViewContactPhone.text = editTextContactPhone.text
        }

        if (originalLogoUri != null) {
            Glide.with(this)
                .load(originalLogoUri)
                .placeholder(R.drawable.default_logo)
                .error(R.drawable.default_logo)
                .into(imageViewLogo)
            imageViewLogo.tag = originalLogoUri  // Restablecer la etiqueta
        } else {
            // Restablece a imagen por defecto o alguna lógica similar
            imageViewLogo.setImageResource(R.drawable.default_logo)
        }

        /*textViewSupplierName.text = editTextSupplierName.text
        textViewContactName.text = editTextContactName.text
        textViewContactPhone.text = editTextContactPhone.text*/

        textViewSupplierName.visibility = View.VISIBLE
        editTextSupplierName.visibility = View.GONE
        textViewContactName.visibility = View.VISIBLE
        editTextContactName.visibility = View.GONE
        textViewContactPhone.visibility = View.VISIBLE
        editTextContactPhone.visibility = View.GONE
        addSupplier.visibility = View.VISIBLE
        deleteSupplier.visibility = View.VISIBLE
        buttonDeleteSupplier.visibility = View.VISIBLE

        imageViewLogo.setOnClickListener(null) // Remover el listener

        buttonConfirmChanges.visibility = View.GONE
        editButton.setImageResource(R.drawable.baseline_edit_24_gray)
    }


    private fun deleteInvoices(selectedIndices: Set<Int>, adapter: InvoiceAdapter) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users/$currentUserUid/suppliers/${intent.getStringExtra("supplierId")}/invoices")

        // Suponemos que InvoiceAdapter maneja una lista de IDs de facturas junto con las URLs
        selectedIndices.forEach { index ->
            val invoiceId = adapter.getInvoiceId(index)  // Necesitas implementar este método en InvoiceAdapter
            databaseRef.child(invoiceId).removeValue().addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Factura eliminada correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al eliminar la factura", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Actualiza el adaptador quitando los ítems eliminados
        adapter.removeItems(selectedIndices)
        adapter.toggleSelectMode() // Desactivar modo de selección
        buttonDelete.visibility = View.GONE
        buttonCancel.visibility = View.GONE
        addSupplier.visibility = View.VISIBLE
        deleteSupplier.visibility = View.VISIBLE
    }
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar este proveedor y todas sus facturas?")
            .setPositiveButton("Eliminar") { dialog, which ->
                deleteSupplier()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun deleteSupplier() {
        val supplierId = intent.getStringExtra("supplierId") ?: return
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val supplierRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("users/$currentUserUid/suppliers/$supplierId")

        // Eliminar el proveedor de Firebase
        supplierRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Proveedor eliminado correctamente", Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            } else {
                Toast.makeText(this, "Error al eliminar el proveedor: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun navigateToDashboard() {
        // Cambia esto según cómo navegas en tu aplicación
        val intent = Intent(this, DashboardFragment::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }


    private fun loadSupplierDetails(supplierId: String) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val supplierRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("users/$currentUserUid/suppliers/$supplierId")

        supplierRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtener los datos del proveedor desde dataSnapshot y asignarlos a las vistas
                    val supplierName = dataSnapshot.child("name").getValue(String::class.java)
                    val supplierLogoUrl = dataSnapshot.child("logoUrl").getValue(String::class.java)
                    val contactName = dataSnapshot.child("contactName").getValue(String::class.java)
                    val contactPhone = dataSnapshot.child("contactPhone").getValue(String::class.java)
                    val registrationDate = dataSnapshot.child("registrationDate").getValue(String::class.java)

                    val textViewSupplierName = findViewById<TextView>(R.id.textViewSupplierName)
                    textViewSupplierName.text = supplierName
                    val textViewContactName = findViewById<TextView>(R.id.textViewContactName)
                    textViewContactName.text = contactName ?: ""
                    val textViewContactPhone = findViewById<TextView>(R.id.textViewContactPhone)
                    textViewContactPhone.text = contactPhone
                    val textViewRegistrationDate = findViewById<TextView>(R.id.textViewRegistrationDate)
                    textViewRegistrationDate.text = getString(R.string.supplier_since) + " " + registrationDate

                    // Cargar las imágenes de las facturas desde la subrama 'invoices'
                    val invoicesSnapshot = dataSnapshot.child("invoices")
                    val invoices = mutableListOf<Invoice>()
                    invoicesSnapshot.children.forEach { invoiceSnapshot ->
                        val id = invoiceSnapshot.key ?: ""
                        val imageUrl = invoiceSnapshot.child("url").getValue(String::class.java) ?: ""
                        val date = invoiceSnapshot.child("date").getValue(String::class.java) ?: "No Date"
                        val time = invoiceSnapshot.child("time").getValue(String::class.java) ?: ""

                        // Convertir la fecha al formato que pueda ser ordenado cronológicamente
                        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                        val newDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        var formattedDate = ""
                        try {
                            val parsedDate = dateFormat.parse(date)
                            formattedDate = newDateFormat.format(parsedDate)
                        } catch (e: ParseException) {
                            e.printStackTrace()  // Handle the case where the date is incorrectly formatted or null
                        }
                        val dateTime = "$formattedDate $time"  // Concatenate date and time for sorting

                        if (imageUrl.isNotEmpty()) {
                            invoices.add(Invoice(id, imageUrl, date, time, dateTime))
                        }
                    }
                    val textViewEmpty = findViewById<TextView>(R.id.textViewEmpty)

                    if(invoices.isEmpty()){
                        gridView.visibility = View.GONE
                        textViewEmpty.visibility = View.VISIBLE
                        deleteSupplier.visibility = View.GONE
                    }else{
                        textViewEmpty.visibility = View.GONE
                        gridView.visibility = View.VISIBLE
                        deleteSupplier.visibility = View.VISIBLE
                    }
                    // Ordena las facturas por fecha y hora de manera descendente
                    invoices.sortByDescending { it.dateTime }

                    val gridView = findViewById<GridView>(R.id.gridViewInvoices)
                    val adapter = InvoiceAdapter(this@SupplierActivity, this@SupplierActivity, invoices)
                    gridView.adapter = adapter

                    // Cargar la imagen del logo del proveedor usando Glide
                    loadLogo(supplierLogoUrl)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors
            }
        })
    }



    private fun loadLogo(imageUrl: String?) {
        val imageViewLogo = findViewById<ImageView>(R.id.imageViewLogo)
        if (!imageUrl.isNullOrEmpty()) {
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.default_logo)
                    .error(R.drawable.default_logo)
                    .into(imageViewLogo)
                imageViewLogo.tag = uri // Guarda la URI en la tag del ImageView
            }.addOnFailureListener {
                imageViewLogo.setImageResource(R.drawable.default_logo) // Image load failure
                imageViewLogo.tag = null
            }
        } else {
            imageViewLogo.setImageResource(R.drawable.default_logo) // Default image if URL is empty or not proper
            imageViewLogo.tag = null
        }
    }

    private fun showFullImageDialog(imageUrl: String) {
        val dialog = Dialog(this, R.style.FullScreenDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_full_image)

        val fullImageView = dialog.findViewById<it.sephiroth.android.library.imagezoom.ImageViewTouch>(R.id.fullImageView)
        val closeButton = dialog.findViewById<ImageView>(R.id.closeButton)

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.default_logo)
            .error(R.drawable.default_logo)
            .into(fullImageView)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }




    fun dismissDialog(view: View) {
        if (view.context is Dialog) {
            (view.context as Dialog).dismiss()
        }
    }





    data class Invoice(
        val id: String = "", // Proporciona un valor predeterminado para cada parámetro
        val imageUrl: String = "",
        val date: String = "",
        val time: String = "",
        val dateTime: String = ""  // This will be used for sorting

    )

    class InvoiceAdapter(private val context: Context, private val activity: SupplierActivity, private var invoices: MutableList<Invoice>) : BaseAdapter() {
        var selectMode = false
        private val selectedItems = mutableSetOf<Int>()
        override fun getCount(): Int = invoices.size
        override fun getItem(position: Int): Invoice = invoices[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val imageView: ImageView
            val checkBox: CheckBox
            val holder: ViewHolder
            val invoice = getItem(position)
            val textViewDate: TextView



            if (convertView == null) {
                view = LayoutInflater.from(context).inflate(R.layout.invoice_image_item, parent, false)
                imageView = view.findViewById(R.id.imageViewInvoice)
                checkBox = view.findViewById(R.id.checkBoxSelect)
                textViewDate = view.findViewById<TextView>(R.id.textViewDate)
                holder = ViewHolder(imageView, checkBox, textViewDate)
                view.tag = holder

            } else {
                holder = convertView.tag as ViewHolder
                view = convertView
                imageView = holder.imageView
                checkBox = holder.checkBox
            }
            println("Attempting to load image URL: ${invoice}")  // Confirmar qué URL se está intentando cargar

            Glide.with(context)
                .load(invoice.imageUrl)
                .placeholder(R.drawable.default_logo)
                .error(R.drawable.default_logo)
                .into(imageView)
            holder.textViewDate.text = invoice.date  // Asegúrate de que tu clase Invoice tenga un campo 'date'

            holder.checkBox.visibility = if (selectMode) View.VISIBLE else View.GONE
            holder.checkBox.isChecked = selectedItems.contains(position)
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedItems.add(position)
                else selectedItems.remove(position)
            }

            imageView.setOnClickListener {
                activity.showFullImageDialog(invoice.imageUrl)
            }




            return view
        }
        fun toggleSelectMode() {
            selectMode = !selectMode
            notifyDataSetChanged()
        }

        fun getSelectedItems(): Set<Int> {
            return selectedItems
        }
        fun getInvoiceId(index: Int): String {
            return invoices[index].id
        }

        fun removeItems(selectedIndices: Set<Int>) {
            selectedIndices.sortedDescending().forEach {
                invoices.removeAt(it)
            }
            selectedItems.clear()  // Limpia la selección después de eliminar
            notifyDataSetChanged()
        }

        data class ViewHolder(val imageView: ImageView, val checkBox: CheckBox, val textViewDate: TextView)
    }





}


