package com.gastrosan.activities.ui.dashboard

import android.os.Bundle
import com.gastrosan.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.SearchView
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.gastrosan.databinding.FragmentDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.widget.ListView
import Suppliers
import android.widget.ArrayAdapter
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import android.widget.Filterable
import android.widget.Filter
import java.util.ArrayList
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.File
import com.bumptech.glide.load.engine.cache.DiskCache



class DashboardFragment : Fragment() {

    private lateinit var listViewProviders: ListView
    private lateinit var searchView: SearchView
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var valueEventListener: ValueEventListener


    private var email: String? = null

    companion object {
        fun newInstance(email: String): DashboardFragment {
            val fragment = DashboardFragment()
            val args = Bundle()
            args.putString("email", email)
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)

        listViewProviders = root.findViewById(R.id.listViewProviders)
        searchView = root.findViewById(R.id.searchView)
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        //receive email from menu
        email = requireActivity().intent.getStringExtra("email")
        println("Email en Dashboard: $email")

        loadSuppliers()

        //searchview
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            /*override fun onQueryTextChange(newText: String?): Boolean {
                (listViewProviders.adapter as CustomAdapter).filter.filter(newText)
                return false
            }*/
            override fun onQueryTextChange(newText: String?): Boolean {
                val adapter = listViewProviders.adapter
                if (adapter is CustomAdapter) {
                    adapter.filter.filter(newText)
                } else {
                    println("Adapter is not an instance of CustomAdapter or is null")
                }
                return false
            }


        })
        return root
    }

    private fun loadSuppliers() {
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

                    // Crear el adaptador personalizado
                    val adapter = CustomAdapter(requireContext(), R.layout.list_item_provider, providerList)
                    listViewProviders.adapter = adapter
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

    override fun onDestroyView() {
        super.onDestroyView()
        // Remover el event listener de Firebase
        val rootRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/")
        rootRef.getReference("users").removeEventListener(valueEventListener)
    }


    private class CustomAdapter(
        context: android.content.Context,
        resource: Int,
        objects: ArrayList<Suppliers>
    ) : ArrayAdapter<Suppliers>(context, resource, objects), Filterable {

        private var supplierList: ArrayList<Suppliers> = objects
        private var filteredSupplierList: ArrayList<Suppliers> = objects
        private var originalSupplierList: ArrayList<Suppliers> = ArrayList(objects)
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val inflater = context.getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val row = inflater.inflate(R.layout.list_item_provider, parent, false)

            if(position >=0 && position < filteredSupplierList.size){
                // Obtener el objeto Supplier en la posición dada
                val supplier = getItem(position)

                // Enlazar las vistas
                val providerName = row.findViewById(R.id.providerName) as TextView
                val providerLogo = row.findViewById(R.id.providerLogo) as ImageView

                // Obtener el proveedor desde la lista filtrada
                val filteredSupplier = filteredSupplierList[position]

                // Asignar los valores del objeto Supplier a las vistas
                providerName.text = filteredSupplier.name

                val sp = filteredSupplier.name

                // Descargar y mostrar la imagen del logo del proveedor desde Firebase Storage
                val logoUrl = filteredSupplier.logoUrl
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
        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val filteredResults = FilterResults()
                    val searchText = constraint.toString().toLowerCase()

                    // Crear una lista para almacenar los resultados filtrados
                    val resultList = ArrayList<Suppliers>()

                    // Iterar sobre la lista original de proveedores y agregar los que coincidan con el texto de búsqueda
                    for (supplier in supplierList) {
                        val name = supplier.name?.toLowerCase()

                        // Verificar si el nombre del proveedor contiene el texto de búsqueda
                        if (name != null && name.contains(searchText)) {
                            resultList.add(supplier)
                        }
                    }

                    // Asignar los resultados filtrados y su tamaño a los resultados del filtro
                    filteredResults.values = resultList
                    filteredResults.count = resultList.size
                    return filteredResults

                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    try {
                        // Limpiar la lista filtrada antes de agregar los resultados del filtro
                        filteredSupplierList.clear()

                        if (constraint.isNullOrEmpty() ) {
                            // Si el filtro está vacío, mostrar toda la lista original de proveedores
                            filteredSupplierList.addAll(originalSupplierList)
                        } else {
                            // Si hay resultados después de aplicar el filtro, agregarlos a la lista filtrada
                            if (results != null && results.count > 0) {
                                filteredSupplierList.addAll(results.values as ArrayList<Suppliers>)
                            }
                        }

                        // Notificar al adaptador después de actualizar la lista filtrada
                        notifyDataSetChanged()
                    } catch (e: Exception) {
                        println("Error al publicar resultados: $e")
                    }
                }


            }
        }
    }
}

