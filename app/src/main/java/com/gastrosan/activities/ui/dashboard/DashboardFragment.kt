package com.gastrosan.activities.ui.dashboard

import android.os.Bundle
import com.gastrosan.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.widget.ListView
import Suppliers
import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.widget.ArrayAdapter
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import android.widget.Filterable
import android.widget.Filter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updatePadding
import java.util.ArrayList
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.gastrosan.activities.AddSupplierActivity
import com.gastrosan.activities.SupplierActivity
import java.util.Locale
import com.airbnb.lottie.LottieAnimationView

class DashboardFragment : Fragment() {

    private lateinit var listViewProviders: ListView
    private lateinit var searchView: SearchView
    private lateinit var addSupplier: ImageView
    private lateinit var deleteSupplier: ImageView
    private lateinit var buttonCancel: Button
    private lateinit var buttonDelete: Button
    private lateinit var noProvidersMessage: TextView
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var valueEventListener: ValueEventListener

    // Variable de instancia para la lista de proveedores
    private var providerList: ArrayList<Suppliers> = arrayListOf()

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
        addSupplier = root.findViewById(R.id.addSupplier)
        deleteSupplier = root.findViewById(R.id.deleteSupplier)
        buttonCancel = root.findViewById(R.id.buttonCancel)
        buttonDelete = root.findViewById(R.id.buttonDelete)
        noProvidersMessage = root.findViewById(R.id.noProvidersMessage) // Inicializa la vista
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Ajustar los parámetros del SearchView para mover la lupa a la derecha
        searchView.setIconifiedByDefault(false)
        searchView.maxWidth = Integer.MAX_VALUE

        // Obtener los iconos del SearchView
        val searchIcon: ImageView = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon)
        val searchClose: ImageView = searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
        val searchPlate: ViewGroup = searchView.findViewById(androidx.appcompat.R.id.search_plate)

        // Configurar padding para que el texto esté alineado a la izquierda
        searchPlate.updatePadding(left = 0, right = 0)
        searchIcon.updatePadding(left = 0, right = 16) // Ajusta el padding según sea necesario

        // Asegurarse de que el botón de cierre esté visible
        searchClose.visibility = ImageView.VISIBLE

        //receive email from menu
        val currentUser = FirebaseAuth.getInstance().currentUser
        email = currentUser?.email
        println("Email en Dashboard: $email")

        loadSuppliers()

        // En tu DashboardFragment
        addSupplier.setOnClickListener {
            val intent = Intent(activity, AddSupplierActivity::class.java)
            startActivity(intent)
        }


        deleteSupplier.setOnClickListener {
            if (providerList.isNotEmpty()) {
                addSupplier.visibility = View.GONE
                deleteSupplier.visibility = View.GONE
                buttonDelete.visibility = View.VISIBLE
                buttonCancel.visibility = View.VISIBLE
                switchAdapter(true)
            } else {
                Toast.makeText(context, "Los datos aún no están cargados, por favor espere.", Toast.LENGTH_SHORT).show()
            }
        }
        buttonDelete.setOnClickListener {
            val adapter = listViewProviders.adapter as? CustomSelectableAdapter
            if (adapter?.getSelectedSuppliers()?.isEmpty() == true) {
                Toast.makeText(context, "Seleccione al menos un proveedor para eliminar.", Toast.LENGTH_SHORT).show()
            } else {
                context?.let { it1 ->
                    AlertDialog.Builder(it1)
                        .setTitle("Confirmar eliminación")
                        .setMessage("¿Estás seguro de que quieres eliminar los proveedores seleccionados?")
                        .setPositiveButton("Eliminar") { dialog, _ ->
                            adapter?.getSelectedSuppliers()?.forEach { supplierId ->
                                deleteSupplierFromFirebase(supplierId)
                            }
                            dialog.dismiss()
                            switchAdapter(false) // Switch back to normal adapter
                            addSupplier.visibility = View.VISIBLE
                            deleteSupplier.visibility = View.VISIBLE
                            buttonDelete.visibility = View.GONE
                            buttonCancel.visibility = View.GONE
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        }
        buttonCancel.setOnClickListener {
            addSupplier.visibility = View.VISIBLE
            deleteSupplier.visibility = View.VISIBLE
            buttonDelete.visibility = View.GONE
            buttonCancel.visibility = View.GONE
            switchAdapter(false) // Cambia de vuelta al adaptador original
        }
        //searchview
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val adapter = listViewProviders.adapter
                if (adapter is Filterable) {  // Asegúrate de que el adaptador implemente Filterable
                    (adapter as Filterable).filter.filter(newText)
                } else {
                    println("Adapter is not an instance of Filterable or is null")
                }
                return false
            }
        })

        listViewProviders.adapter = CustomAdapter(requireContext(), R.layout.list_item_provider, providerList)
        listViewProviders.setOnItemClickListener { parent, view, position, id ->
            val supplier = parent.adapter.getItem(position) as Suppliers
            val intent = Intent(context, SupplierActivity::class.java)
            intent.putExtra("supplierId", supplier.id) // Suponiendo que Suppliers tiene un campo id
            startActivity(intent)
        }

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
                    providerList.clear() // Limpiar lista anterior

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

                    // Mostrar u ocultar el mensaje de "No hay proveedores" según sea necesario
                    if (providerList.isEmpty()) {
                        listViewProviders.visibility = View.GONE
                        noProvidersMessage.visibility = View.VISIBLE
                        searchView.visibility = View.GONE // Ocultar SearchView si no hay proveedores
                    } else {
                        listViewProviders.visibility = View.VISIBLE
                        noProvidersMessage.visibility = View.GONE
                        searchView.visibility = View.VISIBLE // Mostrar SearchView si hay proveedores
                    }

                    listViewProviders.adapter = CustomAdapter(requireContext(), R.layout.list_item_provider, providerList)

                } else {
                    println("No existe usuario con este correo electrónico.")
                    listViewProviders.visibility = View.GONE
                    noProvidersMessage.visibility = View.VISIBLE
                    searchView.visibility = View.GONE // Ocultar SearchView si no hay proveedores
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
    override fun onResume() {
        super.onResume()
        // Recargar los proveedores cuando el fragmento vuelve a estar activo
        loadSuppliers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remover el event listener de Firebase
        val rootRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/")
        rootRef.getReference("users").removeEventListener(valueEventListener)
    }

    fun switchAdapter(isSelectionMode: Boolean) {
        if (isSelectionMode) {
            val selectableAdapter = CustomSelectableAdapter(requireContext(), providerList)
            listViewProviders.adapter = selectableAdapter
        } else {
            val normalAdapter = CustomAdapter(requireContext(), R.layout.list_item_provider, providerList)
            listViewProviders.adapter = normalAdapter
        }
    }

    class CustomSelectableAdapter(
        context: Context,
        objects: ArrayList<Suppliers>
    ) : ArrayAdapter<Suppliers>(context, 0, objects), Filterable {

        private var suppliersList: ArrayList<Suppliers> = objects
        private var filteredSuppliersList: ArrayList<Suppliers> = ArrayList(objects)
        private val selectedSuppliers = HashSet<String>()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_provider3, parent, false)
            val supplier = getItem(position) ?: return view

            val checkBox = view.findViewById<CheckBox>(R.id.checkBox)
            val providerName = view.findViewById<TextView>(R.id.providerName)
            val providerLogo = view.findViewById<ImageView>(R.id.providerLogo)

            providerName.text = supplier.name
            checkBox.isChecked = selectedSuppliers.contains(supplier.id)

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    supplier.id?.let { selectedSuppliers.add(it) }
                } else {
                    supplier.id?.let { selectedSuppliers.remove(it) }
                }
            }

            if (supplier.logoUrl.isNullOrEmpty()) {
                providerLogo.setImageResource(R.drawable.default_logo)
            } else {
                // Si la URL del logo empieza con "gs://", entonces es una referencia de Firebase Storage
                if (supplier.logoUrl!!.startsWith("gs://")) {
                    val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(
                        supplier.logoUrl!!
                    )
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        Glide.with(context)
                            .load(uri)
                            .placeholder(R.drawable.default_logo)
                            .error(R.drawable.default_logo)
                            .into(providerLogo)
                    }.addOnFailureListener {
                        providerLogo.setImageResource(R.drawable.default_logo) // Imagen de fallo
                    }
                } else {
                    // Carga directa si no es una referencia de Firebase
                    Glide.with(context)
                        .load(supplier.logoUrl)
                        .placeholder(R.drawable.default_logo)
                        .error(R.drawable.default_logo)
                        .into(providerLogo)
                }
            }

            return view
        }

        override fun getCount(): Int = filteredSuppliersList.size

        override fun getItem(position: Int): Suppliers? = filteredSuppliersList[position]

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val charString = constraint.toString().ifEmpty { "" }
                    filteredSuppliersList = if (charString.isEmpty()) {
                        suppliersList
                    } else {
                        val filteredList = ArrayList<Suppliers>()
                        for (row in suppliersList) {
                            if (row.name?.lowercase(Locale.ROOT)?.contains(charString.lowercase(
                                    Locale.ROOT
                                ))!!) {
                                filteredList.add(row)
                            }
                        }
                        filteredList
                    }
                    val filterResults = FilterResults()
                    filterResults.values = filteredSuppliersList
                    return filterResults
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    filteredSuppliersList = results?.values as ArrayList<Suppliers>
                    notifyDataSetChanged()
                }
            }
        }

        fun getSelectedSuppliers(): HashSet<String> {
            return selectedSuppliers
        }
    }

    private fun deleteSupplierFromFirebase(supplierId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val databaseRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users/$userId/suppliers/$supplierId")
        databaseRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Proveedor eliminado correctamente", Toast.LENGTH_SHORT).show()
                loadSuppliers() // Reload the suppliers from Firebase
            } else {
                Toast.makeText(context, "Error al eliminar proveedor", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class CustomAdapter(
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

            if (position >= 0 && position < filteredSupplierList.size) {
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
                    val searchText = constraint.toString().lowercase(Locale.getDefault())

                    // Crear una lista para almacenar los resultados filtrados
                    val resultList = ArrayList<Suppliers>()

                    // Iterar sobre la lista original de proveedores y agregar los que coincidan con el texto de búsqueda
                    for (supplier in supplierList) {
                        val name = supplier.name?.lowercase(Locale.getDefault())

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

                        if (constraint.isNullOrEmpty()) {
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
