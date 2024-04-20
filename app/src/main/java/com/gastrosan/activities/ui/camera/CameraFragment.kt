package com.gastrosan.activities.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity



class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture

    private lateinit var rotateButton: Button
    private lateinit var saveButton: Button

    private var isCameraInitialized = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Inicialización de outputDirectory
        initializeOutputDirectory()

        rotateButton = root.findViewById(R.id.btnRotate)
        saveButton = root.findViewById(R.id.btnGuardar)

        rotateButton.setOnClickListener {
            // Aumenta la rotación actual en 90 grados
            val currentRotation = binding.imageView.rotation
            binding.imageView.rotation = currentRotation + 90
        }

        //binding.btnCamara.isEnabled = false  // Deshabilita el botón inicialmente

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            println("Permissions granted1.0")
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.btnCamara.setOnClickListener { takePhoto() }
        binding.btnGaleria.setOnClickListener { openGallery() }

        return root
    }

    private fun initializeOutputDirectory() {
        val mediaDir = requireContext().externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.gastrosan)).apply { mkdirs() }
        }
        outputDirectory = if (mediaDir != null && mediaDir.exists()) mediaDir else requireContext().filesDir
    }

    // Asegúrate de tener los permisos de CAMERA y WRITE_EXTERNAL_STORAGE
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            println("Camera provider ready")
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(binding.viewFinder.display.rotation)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                isCameraInitialized = true
                println("Camera initialized")
                activity?.runOnUiThread {
                    binding.btnCamara.isEnabled = true
                }
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                isCameraInitialized = false
                activity?.runOnUiThread {
                    binding.btnCamara.isEnabled = false
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }


    override fun onPause() {
        super.onPause()
        //binding.btnCamara.isEnabled = false // Deshabilita el botón cuando la vista no está activa
    }

    override fun onResume() {
        super.onResume()
        if (isCameraInitialized) {
            binding.btnCamara.isEnabled = true
        }
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


    private fun takePhoto() {
        // Verifica que imageCapture no sea nulo antes de utilizarlo
        if (::imageCapture.isInitialized) {
            // Asegúrate de que el viewFinder esté visible cuando tomas una foto
            binding.viewFinder.visibility = View.VISIBLE
            binding.imageView.visibility = View.GONE

            val photoFile = File(
                outputDirectory,
                SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        val msg = "Photo capture succeeded: $savedUri"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        // Opcional: Mostrar la imagen en la ImageView (esto ocultará la vista previa)
                        binding.imageView.visibility = View.VISIBLE
                        binding.viewFinder.visibility = View.GONE
                        binding.imageView.setImageURI(savedUri)

                        // Ocultar botones de cámara y galería, mostrar botón de guardar
                        binding.btnCamara.visibility = View.GONE
                        binding.btnGaleria.visibility = View.GONE
                        binding.btnGuardar.visibility = View.VISIBLE
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }
                }
            )
        } else {
            Log.e(TAG, "Error: imageCapture no ha sido inicializado")
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                println("Permissions granted")
                startCamera()
            } else {
                Toast.makeText(context, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                requireActivity().finish()
            }
        }
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickFromGallery.launch(intent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {
        private const val TAG = "CameraFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private lateinit var outputDirectory: File
    }
}
