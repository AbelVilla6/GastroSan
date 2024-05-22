package com.gastrosan.activities.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.gastrosan.R
import com.gastrosan.databinding.FragmentCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import android.provider.MediaStore
import android.app.Activity.RESULT_OK
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.IOException
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.provider.Settings
import android.content.Context
import android.util.Log
import android.widget.ListView
import android.widget.ArrayAdapter
import android.widget.Filterable
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import Suppliers
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.TypedValue
import android.widget.CheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updateLayoutParams
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.gastrosan.activities.ui.dashboard.DashboardFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.airbnb.lottie.LottieAnimationView
import com.gastrosan.activities.AddSupplierActivity


class CameraFragment : Fragment() {
    private var selectedIndex: Int = -1  // -1 significa que no hay selección
    private var isLoadingData = true // Variable para controlar el estado de carga de los datos
    private var currentRotation = 0f  // Guardar el estado actual de la rotación

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture

    private lateinit var rotateButton: Button
    private lateinit var saveButton: Button
    private lateinit var nextButton: Button
    private lateinit var addButton: Button
    private lateinit var selectTextView: TextView
    private lateinit var btnAddSupplier: Button

    private lateinit var outputDirectory: File
    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var currentPhotoPath: String

    // Usamos constantes para los códigos de permisos
    private val REQUEST_CAMERA_PERMISSION = 100
    private val REQUEST_GALLERY_PERMISSION = 101

    private lateinit var listViewProviders: ListView
    private lateinit var valueEventListener: ValueEventListener

    private lateinit var database: DatabaseReference
    private var email: String? = null

    private val addSupplierActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Actualiza la lista de proveedores cuando se vuelve de AddSupplierActivity
            loadSuppliers()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        val root: View = binding.root

        cameraExecutor = Executors.newSingleThreadExecutor()
        initializeOutputDirectory()

        //receive email from menu
        val currentUser = FirebaseAuth.getInstance().currentUser
        email = currentUser?.email
        println("Email en Dashboard: $email")

        // Botón para tomar foto
        binding.btnCamara.setOnClickListener {
            vibrateButton(requireContext())
            requestCameraPermission()  // Llamada al método actualizado
        }

        // Botón para abrir la galería
        binding.btnGaleria.setOnClickListener {
            vibrateButton(requireContext())
            //requestGalleryPermission()  // Llamada al método actualizado
            openGallery()
        }

        rotateButton = root.findViewById(R.id.btnRotate)
        saveButton = root.findViewById(R.id.btnGuardar)
        addButton = root.findViewById(R.id.btnAddSupplier)
        nextButton = root.findViewById(R.id.btnSiguiente)
        btnAddSupplier = root.findViewById(R.id.btnAddSupplier)
        selectTextView = root.findViewById(R.id.textSelectProvider)
        listViewProviders = root.findViewById(R.id.listViewProviders)

        rotateButton.setOnClickListener {
            vibrateButton(requireContext())
            // Aumenta la rotación actual en 90 grados
            currentRotation += 90
            if (currentRotation == 360f) currentRotation = 0f  // Restablecer después de 360 grados
            binding.imageView.rotation = currentRotation
        }

        setupListView()

        startCamera()

        btnAddSupplier.setOnClickListener{
            vibrateButton(requireContext())
            val intent = Intent(requireContext(), AddSupplierActivity::class.java)
            addSupplierActivityResultLauncher.launch(intent)
        }
        return root
    }
    fun vibrateButton(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Para dispositivos con API 26 o superior
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // Para dispositivos con API menor a 26
            vibrator.vibrate(20)
        }
    }
    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permisos Necesarios")
            .setMessage("Esta aplicación necesita permisos para continuar. Puedes otorgarlos en Ajustes.")
            .setPositiveButton("Ir a Ajustes") { dialog, _ ->
                dialog.cancel()
                openAppSettings()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }
    private fun setupListView() {
        val adapter = CustomAdapter(requireContext(), R.layout.list_item_provider2, ArrayList())
        listViewProviders.adapter = adapter
        listViewProviders.choiceMode = ListView.CHOICE_MODE_SINGLE
        listViewProviders.setOnItemClickListener { parent, view, position, id ->
            if (!isLoadingData && position >= 0 && position < adapter.count) {
                adapter.selectedPosition = position
                listViewProviders.setItemChecked(position, true)
                adapter.notifyDataSetChanged()
                Toast.makeText(context, "Proveedor seleccionado: ${adapter.getItem(position)?.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Por favor, marca el checkbox correspondiente", Toast.LENGTH_SHORT).show()
            }
        }
    }






    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
    }
    private fun wasPermissionRequested(permission: String): Boolean {
        return requireContext().getSharedPreferences("PermissionPrefs", Context.MODE_PRIVATE)
            .getBoolean(permission, false)
    }

    private fun markPermissionRequested(permission: String) {
        requireContext().getSharedPreferences("PermissionPrefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(permission, true)
            .apply()
    }
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) || !wasPermissionRequested(Manifest.permission.CAMERA)) {
                markPermissionRequested(Manifest.permission.CAMERA)
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            } else {
                showSettingsDialog()
            }
        } else {
            takePhoto()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(context, "Failed to start camera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Método para inicializar el ListView y cargar los proveedores
    private fun setupProvidersList() {
        listViewProviders = binding.listViewProviders
        //listViewProviders.visibility = View.VISIBLE // Hacer visible el ListView
        loadSuppliers() // Cargar los proveedores
    }

    // Método para cargar los proveedores desde Firebase
    private fun loadSuppliers() {
        isLoadingData = true // Asegúrate de que estamos en estado de carga
        // Obtener la referencia de la base de datos de Firebase
        val rootRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/")
        val usersRef = rootRef.getReference("users")

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!isAdded) { // Verifica si el fragmento todavía está adjunto
                    return
                }
                if (dataSnapshot.exists()) {
                    val providerList = ArrayList<Suppliers>()

                    for (userSnapshot in dataSnapshot.children) {
                        val suppliersSnapshot = userSnapshot.child("suppliers")

                        suppliersSnapshot.children.forEach { providerSnapshot ->
                            val id = providerSnapshot.child("id").getValue(String::class.java)
                            val name = providerSnapshot.child("name").getValue(String::class.java)
                            val logoUrl = providerSnapshot.child("logoUrl").getValue(String::class.java)
                            val contactName = providerSnapshot.child("contactName").getValue(String::class.java)
                            val contactPhone = providerSnapshot.child("contactPhone").getValue(String::class.java)
                            println("Proveedor: $id, $name, $logoUrl, $contactName, $contactPhone")

                            val supplier = Suppliers(id, name, logoUrl, contactName, contactPhone)
                            providerList.add(supplier)
                        }
                    }

                    // Crear el adaptador personalizado, ahora incluyendo selectedIndex
                    val adapter = CameraFragment.CustomAdapter(
                        requireContext(),
                        R.layout.list_item_provider2,
                        providerList // Asegúrate de pasar selectedIndex aquí
                    )
                    listViewProviders.adapter = adapter
                    isLoadingData = false // Datos cargados, ajustar estado
                    println("ListView should now be visible")

                } else {
                    println("No existe usuario con este correo electrónico.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) { // Verifica si el fragmento todavía está adjunto
                    return
                }
                println("Error al cargar proveedores: ${error.message}")
            }
        }
        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(valueEventListener)
    }


    private fun takePhoto() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Asegúrate de que hay una aplicación de cámara que pueda manejar el intent
            takePictureIntent.resolveActivity(requireContext().packageManager)?.also {
                // Crea el archivo donde irá la foto
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error ocurrido mientras se creaba el archivo
                    Toast.makeText(requireContext(), "Error al crear el archivo de imagen: ${ex.message}", Toast.LENGTH_SHORT).show()
                    null
                }
                // Continúa solo si el archivo fue creado exitosamente
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.gastrosan.fileprovider", // Ajusta al nombre de tu FileProvider definido en el manifest
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Crea un nombre de archivo de imagen
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Guarda un archivo: path para usar con ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            // Muestra la imagen en un ImageView
            val imgUri = Uri.fromFile(File(currentPhotoPath))
            binding.imageView.setImageURI(imgUri)
            binding.viewFinder.visibility = View.GONE  // Ocultar el PreviewView
            // Ocultar botones de cámara y galería, mostrar botón de guardar
            binding.btnCamara.visibility = View.GONE
            binding.btnGaleria.visibility = View.GONE
            binding.btnSiguiente.visibility = View.VISIBLE
            binding.btnRotate.visibility = View.VISIBLE
            setupProvidersList() // Configurar y mostrar la lista de proveedores

            binding.btnSiguiente.setOnClickListener {
                vibrateButton(requireContext())
                prepareScreenThree()
            }

        }
    }

    private fun prepareScreenThree() {
        // Aplicar la rotación almacenada al ImageView
        binding.imageView.rotation = currentRotation

        // Set the layout dynamically based on rotation
        when (currentRotation % 360) {
            0f -> {
                // Reducir el tamaño del ImageView para que sea una miniatura
                val layoutParams = binding.imageView.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.width = convertDpToPixel(200f, requireContext()) // Asumiendo que quieres 100dp de ancho
                layoutParams.height = convertDpToPixel(200f, requireContext()) // Y 100dp de alto para la miniatura
                layoutParams.topMargin = convertDpToPixel(20f, requireContext()) // Añadir un margen superior
                // Actualizar las restricciones para asegurar que la miniatura se muestre correctamente en el layout
                val constraintSet = ConstraintSet()
                constraintSet.clone(binding.constraintLayout)
                // Establece las restricciones para centrar en horizontal y establecer el margen superior
                constraintSet.connect(R.id.imageView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, layoutParams.topMargin)
                constraintSet.connect(R.id.imageView, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
                constraintSet.connect(R.id.imageView, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
                constraintSet.centerHorizontally(R.id.imageView, ConstraintSet.PARENT_ID)
                constraintSet.applyTo(binding.constraintLayout)
            }
            90f -> {
                // 90 degrees rotation
                // Reducir el tamaño del ImageView para que sea una miniatura
                val layoutParams = binding.imageView.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.topMargin = convertDpToPixel(0f, requireContext())
                layoutParams.width = convertDpToPixel(150f, requireContext())
                layoutParams.height = convertDpToPixel(250f, requireContext())
                // Actualizar las restricciones para asegurar que la miniatura se muestre correctamente en el layout
                val constraintSet = ConstraintSet()
                constraintSet.clone(binding.constraintLayout)
                // Establece las restricciones para centrar en horizontal y establecer el margen superior
                constraintSet.connect(R.id.imageView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, layoutParams.topMargin)
                constraintSet.connect(R.id.imageView, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, )
                constraintSet.connect(R.id.imageView, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 1000 )
                constraintSet.centerHorizontally(R.id.imageView, ConstraintSet.PARENT_ID)
                constraintSet.applyTo(binding.constraintLayout)
            }
            180f -> {
                val newWidth = convertDpToPixel(200f, requireContext())
                val newHeight = convertDpToPixel(200f, requireContext())

                // Aplicar cambios en imageView
                binding.imageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    width = newWidth
                    height = newHeight
                    topMargin = 0
                    marginStart = 45
                }

                // Asegurarnos de que imageView esté en la parte superior del layout
                binding.imageView.layoutParams = (binding.imageView.layoutParams as ConstraintLayout.LayoutParams).apply {
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                }

                // Aplicar cambios en viewFinder para que coincida con el tamaño y posicionamiento
                binding.viewFinder.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    width = newWidth
                    height = newHeight
                }

                // Asegurarnos de que viewFinder esté en la misma posición que imageView
                binding.viewFinder.layoutParams = (binding.viewFinder.layoutParams as ConstraintLayout.LayoutParams).apply {
                    topToTop = binding.imageView.id
                    startToStart = binding.imageView.id
                    endToEnd = binding.imageView.id
                }

                binding.imageView.requestLayout()
                binding.viewFinder.requestLayout()
            }
            270f -> {
                val newWidth = convertDpToPixel(200f, requireContext())
                val newHeight = convertDpToPixel(200f, requireContext())

                // Aplicar cambios en imageView
                binding.imageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    width = newWidth
                    height = newHeight
                    topMargin = 0  // Ajusta este valor si necesitas mover la imagen más arriba o más abajo
                    marginStart = 0  // Ajusta según necesites para centrar la imagen horizontalmente
                }

                // Asegurarnos de que imageView esté en la parte superior del layout
                binding.imageView.layoutParams = (binding.imageView.layoutParams as ConstraintLayout.LayoutParams).apply {
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                }

                // Aplicar cambios en viewFinder para que coincida con el tamaño y posicionamiento
                binding.viewFinder.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    width = newWidth
                    height = newHeight
                }

                // Asegurarnos de que viewFinder esté en la misma posición que imageView
                binding.viewFinder.layoutParams = (binding.viewFinder.layoutParams as ConstraintLayout.LayoutParams).apply {
                    topToTop = binding.imageView.id
                    startToStart = binding.imageView.id
                    endToEnd = binding.imageView.id
                }

                binding.imageView.requestLayout()
                binding.viewFinder.requestLayout()
            }
        }
            // Actualiza visibilidades y funciones
        updateUIForScreenThree()

        // Configurar la acción del botón "Guardar"
        binding.btnGuardar.setOnClickListener {
            vibrateButton(requireContext())
            val adapter = listViewProviders.adapter as CustomAdapter
            if (adapter.selectedPosition == -1) {
                // No hay ningún proveedor seleccionado
                Toast.makeText(context, "Debes seleccionar un proveedor o añadir uno nuevo", Toast.LENGTH_LONG).show()
            } else {
                // Hay un proveedor seleccionado
                val selectedSupplier = adapter.getItem(adapter.selectedPosition)

                // Usar el Uri adecuado según el origen de la imagen
                val photoUri = if (this::currentPhotoPath.isInitialized) {
                    Uri.fromFile(File(currentPhotoPath)) // Foto de la cámara
                } else {
                    // Aquí manejas el caso de la galería, asegúrate de tener un Uri válido
                    binding.imageView.tag as? Uri ?: return@setOnClickListener // Usar un tag para guardar el Uri de la imagen seleccionada
                }
                uploadImageToFirebaseStorage(photoUri, selectedSupplier?.id ?: "")

            }
        }

    }
    // Función auxiliar para convertir DP a píxeles
    fun convertDpToPixel(dp: Float, context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }

    private fun updateUIForScreenThree() {
        binding.btnSiguiente.visibility = View.GONE
        binding.btnRotate.visibility = View.GONE
        binding.textSelectProvider.visibility = View.VISIBLE
        binding.listViewProviders.visibility = View.VISIBLE
        binding.btnGuardar.visibility = View.VISIBLE
        binding.btnAddSupplier.visibility = View.VISIBLE

        if (listViewProviders.adapter == null) {
            loadSuppliers()
        }
    }
    fun uploadImageToFirebaseStorage(imageUri: Uri, supplierId: String) {
        showLoadingScreen(true)  // Mostrar la pantalla de carga

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return  // Asegúrate de que el usuario está autenticado antes de continuar

        // Generar un nombre de archivo único usando timestamp o número random
        val timestamp = System.currentTimeMillis()

        // Extraer solo el nombre del archivo del Uri, ignorando cualquier otro path
        val fileName = imageUri.lastPathSegment?.substringAfterLast('/') ?: "default_${System.currentTimeMillis()}.jpg"

        val storageRef = FirebaseStorage.getInstance().reference.child("invoices/$userId/$supplierId/$fileName")
        val uploadTask = storageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                showLoadingScreen(false, true)  // Éxito, muestra el tic verde
                saveImageUrlToDatabase(downloadUri.toString(), supplierId, "invoice_$timestamp")
            }
        }.addOnFailureListener { exception ->
            showLoadingScreen(false, false)  // Error, muestra la cruz roja
            Toast.makeText(context, "Error al subir imagen: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }
    fun showLoadingScreen(show: Boolean, success: Boolean? = null) {
        binding?.let { bind ->
            if (show) {
                binding.listViewProviders.visibility = View.GONE
                binding.textSelectProvider.visibility = View.GONE
                binding.btnGuardar.visibility = View.GONE
                binding.btnAddSupplier.visibility = View.GONE
                binding.lottieAnimationView.visibility = View.VISIBLE

                binding.statusMessage.text = "Guardando imagen..."
                binding.statusMessage.visibility = View.VISIBLE
                binding.lottieAnimationView.setAnimation(R.raw.loading_animation)  // Asume que tienes una animación de carga

                binding.lottieAnimationView.playAnimation()
            } else {
                binding.lottieAnimationView.postDelayed({
                    binding.lottieAnimationView.pauseAnimation()
                    binding.lottieAnimationView.visibility = View.GONE
                    binding.statusMessage.visibility = View.GONE

                }, 5000)  // Detiene la animación después de 8 segundos

                if (success == true) {
                    binding.lottieAnimationView.setAnimation(R.raw.success_animation)
                    binding.statusMessage.text = "La imagen se ha guardado correctamente."
                } else {
                    binding.lottieAnimationView.setAnimation(R.raw.error_animation)
                    binding.statusMessage.text = "La imagen no ha podido guardarse, por favor intentalo de nuevo."
                }
                binding.lottieAnimationView.playAnimation()
                binding.statusMessage.visibility = View.VISIBLE
            }
        }
    }

    fun saveImageUrlToDatabase(imageUrl: String, supplierId: String, invoiceId: String) {
        val databaseRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users")
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()
        val invoiceData = mapOf(
            "url" to imageUrl,
            "timestamp" to timestamp,
            "date" to SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(timestamp)),
            "time" to SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        )
        println("El usuario: $userId, va a almacenar la información de la factura: $invoiceData, para el proveedor: $supplierId")
        databaseRef.child(userId).child("suppliers").child(supplierId).child("invoices").child(invoiceId).setValue(invoiceData)
            .addOnSuccessListener {
                println("Imagen guardada con éxito")
                navigateHome()
            }
            .addOnFailureListener {
                println("Error al guardar la información de la factura: ${it.message}")
            }
    }
    private fun navigateHome() {
        // Handler con un delay de 8 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            // Verifica si el fragmento está correctamente añadido a la actividad y la actividad no está finalizando
            val activity = activity
            if (isAdded && activity != null && !activity.isFinishing) {
                try {
                    // Navegar al HomeFragment si es seguro
                    findNavController().navigate(R.id.navigation_home)
                } catch (e: Exception) {
                    Log.e("CameraFragment", "Error al navegar: ${e.message}")
                }
            } else {
                Log.e("CameraFragment", "El Fragmento no está adjunto o la actividad está finalizando.")
            }
        }, 5000)  // Retraso de 5 segundos
    }

    private fun initializeOutputDirectory() {
        outputDirectory = requireContext().getExternalFilesDir(null) ?: requireContext().filesDir
    }
    private val pickFromGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                binding.imageView.visibility = View.VISIBLE  // Mostrar la ImageView
                binding.imageView.setImageURI(it)  // Cargar la imagen seleccionada
                binding.imageView.tag = it  // Almacenar el Uri en el tag para uso posterior
            }
            binding.viewFinder.visibility = View.GONE  // Ocultar el PreviewView
            //binding.imageView.visibility = View.VISIBLE  // Mostrar la ImageView
            //binding.imageView.setImageURI(imageUri)  // Cargar la imagen seleccionada

            // Ocultar botones de cámara y galería, mostrar botón de guardar
            binding.btnCamara.visibility = View.GONE
            binding.btnGaleria.visibility = View.GONE
            binding.btnSiguiente.visibility = View.VISIBLE
            binding.btnRotate.visibility = View.VISIBLE

            // Configurar y mostrar la lista de proveedores
            setupProvidersList()

            binding.btnSiguiente.setOnClickListener {
                vibrateButton(requireContext())
                prepareScreenThree()
            }
        }
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickFromGallery.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        println("Destroying view.")
        cameraExecutor.shutdown()
        _binding = null
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            } else {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    showSettingsDialog()
                } else {
                    Toast.makeText(context, "Permission for camera was denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_TAKE_PHOTO = 1
    }

    class CustomAdapter(
        context: Context,
        private val resource: Int,
        objects: ArrayList<Suppliers>?
    ) : ArrayAdapter<Suppliers>(context, resource, objects?: ArrayList()) {

        private var supplierList: ArrayList<Suppliers>? = objects
        var selectedPosition = -1  // Añadir variable para mantener la posición seleccionada

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val inflater = context.getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val row = inflater.inflate(R.layout.list_item_provider2, parent, false)

            val providerName = row.findViewById<TextView>(R.id.providerName)
            val providerLogo = row.findViewById<ImageView>(R.id.providerLogo)
            val checkBox = row.findViewById<CheckBox>(R.id.checkBox)

            val supplier = supplierList?.get(position)
            providerName.text = supplier?.name

            // Actualizar el estado del CheckBox basado en la selección actual del ListView
            checkBox.isChecked = position == selectedPosition

            checkBox.setOnClickListener {
                selectedPosition = position
                notifyDataSetChanged()  // Notificar que los datos han cambiado para refrescar la vista
            }

            if(position >=0 && position < supplierList?.size ?: 0){
                // Obtener el objeto Supplier en la posición dada
                val supplier = getItem(position)

                // Enlazar las vistas
                val providerName = row.findViewById(R.id.providerName) as TextView
                val providerLogo = row.findViewById(R.id.providerLogo) as ImageView

                // Obtener el proveedor desde la lista filtrada
                val filteredSupplier = supplierList?.get(position)

                // Asignar los valores del objeto Supplier a las vistas
                providerName.text = filteredSupplier?.name

                val sp = filteredSupplier?.name

                // Descargar y mostrar la imagen del logo del proveedor desde Firebase Storage
                val logoUrl = filteredSupplier?.logoUrl
                if (logoUrl != null && logoUrl.isNotEmpty()) {
                    println("Supplier: $sp, Logo URL: $logoUrl")
                    // Obtener la referencia de Firebase Storage para el URL del logo
                    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(logoUrl)
                    println("StorageRef: $storageRef")
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        println("Image download success, URI: $uri")
                        Glide.with(context)
                            .load(uri)
                            .diskCacheStrategy(DiskCacheStrategy.ALL) // Habilitar almacenamiento en caché
                            .placeholder(R.drawable.default_logo) // Imagen predeterminada mientras se carga
                            .error(R.drawable.default_logo) // Imagen predeterminada en caso de error
                            .into(providerLogo)
                    }.addOnFailureListener { exception ->
                        Log.e("CustomAdapter", "Error al descargar imagen: $exception")
                        println("Image download failed: $exception")
                        providerLogo.setImageResource(R.drawable.default_logo) // Mostrar imagen predeterminada en caso de error
                    }
                } else {
                    // Si no se proporciona URL de imagen o es vacío, mostrar la imagen predeterminada
                    println("No logo URL provided or empty, using default logo")
                    providerLogo.setImageResource(R.drawable.default_logo)
                }
            }
            return row
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedPosition", (listViewProviders.adapter as? CustomAdapter)?.selectedPosition ?: -1)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val position = savedInstanceState?.getInt("selectedPosition", -1) ?: -1
        if (position != -1) {
            (listViewProviders.adapter as? CustomAdapter)?.selectedPosition = position
        }
    }

}
