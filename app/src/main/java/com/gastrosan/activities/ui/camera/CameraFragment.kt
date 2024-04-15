package com.gastrosan.activities.ui.camera

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.gastrosan.R
import com.gastrosan.activities.ui.dashboard.DashboardViewModel
import com.gastrosan.databinding.FragmentCameraBinding
import com.gastrosan.databinding.FragmentDashboardBinding
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.widget.Toast
import android.net.Uri
import java.io.File



class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var btnCamara: Button
    private lateinit var imageView: ImageView
    private lateinit var btnGaleria: Button

    private val REQUEST_CAMERA_PERMISSION = 100

    // Declarar la variable booleana a nivel de clase
    private var isPermissionCameraGranted: Boolean = false
    private var isPermissionGalleryGranted: Boolean = false




    //Mejorar calidad imagen de cámara
    private var photoUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Inicializar vistas
        btnCamara = binding.btnCamara
        btnGaleria = binding.btnGaleria
        imageView = binding.imageView

        checkPermissions()

        btnCamara.setOnClickListener {
            if(isPermissionCameraGranted){
                openCamera()
            }else{
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 777)
            }
        }
        btnGaleria.setOnClickListener {
            if(isPermissionGalleryGranted){
                openGallery()
            }else{
                requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), 778)
            }
        }
        return root
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap?
                if (imageBitmap != null) {
                    imageView.setImageBitmap(imageBitmap)
                }
            }
        }
    private val pickFromGallery =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val imageUri = result.data?.data
                imageView.setImageURI(imageUri)
            }
        }


    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startForResult.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickFromGallery.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkPermissions(){
        if(requireContext().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            // No hay permiso, solicitarlo
            requestCameraPermission()
        }else{
            // Ya hay permiso
            isPermissionCameraGranted = true
        }
        if(requireContext().checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
            // No hay permiso, solicitarlo
            requestGalleryPermission()
        }else{
            // Ya hay permiso
            isPermissionGalleryGranted = true
        }
    }

    private fun requestGalleryPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_MEDIA_IMAGES)){
            // El usuario ya ha rechazado los permisos, Mostrar mensaje de explicación
            Toast.makeText(requireContext(), "Se necesita permiso para acceder a la galería", Toast.LENGTH_SHORT).show()
        }else{
            // Solicitar permiso
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 778)
        }
    }

    private fun requestCameraPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA)){
            // El usuario ya ha rechazado los permisos, Mostrar mensaje de explicación
            Toast.makeText(requireContext(), "Se necesita permiso para acceder a la cámara", Toast.LENGTH_SHORT).show()
        }else{
            // Solicitar permiso
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), 777)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: kotlin.Int,
        permissions: kotlin.Array<out kotlin.String>,
        grantResults: kotlin.IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==777){
            //Nuestros permisos de cámara
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //Permiso concedido
                isPermissionCameraGranted = true
            }else{
                //Permiso denegado
                isPermissionCameraGranted = false
                Toast.makeText(requireContext(), "El permiso de cámara está denegado, por favor, actívelo desde ajustes", Toast.LENGTH_SHORT).show()
            }
        }
        else if(requestCode==778){
            //Nuestros permisos de galería
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //Permiso concedido
                isPermissionGalleryGranted = true
            }else{
                //Permiso denegado
                isPermissionGalleryGranted = false
                Toast.makeText(requireContext(), "El permiso de galería está denegado, por favor, actívelo desde ajustes", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
