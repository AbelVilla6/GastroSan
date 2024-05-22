package com.gastrosan.activities.ui.home

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.gastrosan.R
import com.gastrosan.activities.AddSupplierActivity
import com.gastrosan.activities.ProfileActivity
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private lateinit var layoutConsultarProveedores: LinearLayout
    private lateinit var layoutAnadirFactura: LinearLayout
    private lateinit var layoutAnadirProveedor: LinearLayout
    private lateinit var layoutPerfil: LinearLayout
    private var userEmail: String? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        layoutConsultarProveedores = root.findViewById(R.id.layoutConsultarProveedores)
        layoutAnadirFactura = root.findViewById(R.id.layoutAnadirFactura)
        layoutAnadirProveedor = root.findViewById(R.id.layoutAnadirProveedor)
        layoutPerfil = root.findViewById(R.id.layoutPerfil)

        // Obtener el correo electrónico del usuario actual desde FirebaseAuth
        val currentUser = FirebaseAuth.getInstance().currentUser
        userEmail = currentUser?.email
        println("Email en HomeFragment: $userEmail")

        // Configurar los eventos de click para cada LinearLayout
        layoutConsultarProveedores.setOnClickListener {
            vibrateButton(requireContext())
            navigateTo(R.id.navigation_dashboard)
        }
        layoutAnadirFactura.setOnClickListener {
            vibrateButton(requireContext())
            navigateTo(R.id.navigation_camera)
        }
        layoutAnadirProveedor.setOnClickListener {
            vibrateButton(requireContext())
            startActivity(Intent(context, AddSupplierActivity::class.java))
        }
        layoutPerfil.setOnClickListener {
            vibrateButton(requireContext())
            // Iniciar ProfileActivity pasando el correo electrónico del usuario
            val intent = Intent(context, ProfileActivity::class.java).apply {
                putExtra("email", userEmail)
            }
            startActivity(intent)
        }

        return root
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
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            println("Vibrando API 26+")
        } else {
            // Para dispositivos con API menor a 26
            vibrator.vibrate(50)
            println("Vibrando API menor a 26")
        }
    }

    private fun navigateTo(destinationId: Int) {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.navigation_home, true)
            .build()

        findNavController().navigate(destinationId, null, navOptions)
    }
}
