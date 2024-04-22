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


class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture

    private lateinit var rotateButton: Button
    private lateinit var saveButton: Button

    private lateinit var outputDirectory: File
    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var currentPhotoPath: String

    // Usamos constantes para los códigos de permisos
    private val REQUEST_CAMERA_PERMISSION = 100
    private val REQUEST_GALLERY_PERMISSION = 101

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        val root: View = binding.root

        cameraExecutor = Executors.newSingleThreadExecutor()
        initializeOutputDirectory()

        // Botón para tomar foto
        binding.btnCamara.setOnClickListener {
            requestCameraPermission()  // Llamada al método actualizado
        }

        // Botón para abrir la galería
        binding.btnGaleria.setOnClickListener {
            //requestGalleryPermission()  // Llamada al método actualizado
            openGallery()
        }

        rotateButton = root.findViewById(R.id.btnRotate)
        saveButton = root.findViewById(R.id.btnGuardar)

        rotateButton.setOnClickListener {
            // Aumenta la rotación actual en 90 grados
            val currentRotation = binding.imageView.rotation
            binding.imageView.rotation = currentRotation + 90
        }

        startCamera()
        return root
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
            binding.btnGuardar.visibility = View.VISIBLE
            binding.btnRotate.visibility = View.VISIBLE
        }
    }

    private fun initializeOutputDirectory() {
        outputDirectory = requireContext().getExternalFilesDir(null) ?: requireContext().filesDir
    }
    private val pickFromGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val imageUri = result.data?.data
            binding.viewFinder.visibility = View.GONE  // Ocultar el PreviewView
            binding.imageView.visibility = View.VISIBLE  // Mostrar la ImageView
            binding.imageView.setImageURI(imageUri)  // Cargar la imagen seleccionada

            // Ocultar botones de cámara y galería, mostrar botón de guardar
            binding.btnCamara.visibility = View.GONE
            binding.btnGaleria.visibility = View.GONE
            binding.btnGuardar.visibility = View.VISIBLE
            binding.btnRotate.visibility = View.VISIBLE
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

}
