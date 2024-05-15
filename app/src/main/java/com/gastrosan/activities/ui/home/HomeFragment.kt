package com.gastrosan.activities.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gastrosan.R
import com.gastrosan.activities.AddSupplierActivity


class HomeFragment : Fragment() {

    private lateinit var menuOptionsListView: ListView
    private val menuOptions = arrayOf(
        "Consultar Proveedores",
        "Añadir Proveedor",
        "Añadir Factura"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        menuOptionsListView = root.findViewById(R.id.listMenuOptions)

        // Usamos un ArrayAdapter personalizado para inflar nuestro layout personalizado
        val adapter = object : ArrayAdapter<String>(requireContext(), R.layout.list_item_menu, menuOptions) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: inflater.inflate(R.layout.list_item_menu, parent, false)
                val menuItemText = view.findViewById<TextView>(R.id.menuItemText)
                menuItemText.text = getItem(position)
                return view
            }
        }
        menuOptionsListView.adapter = adapter

        // Configurar los eventos de click para cada ítem del menú
        menuOptionsListView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> findNavController().navigate(R.id.navigation_dashboard)
                1 -> startActivity(Intent(context, AddSupplierActivity::class.java));
                //2 -> Toast.makeText(requireContext(), "Añadir Restaurante", Toast.LENGTH_SHORT).show()
                2 -> findNavController().navigate(R.id.navigation_camera)
            }
        }

        return root
    }
}
