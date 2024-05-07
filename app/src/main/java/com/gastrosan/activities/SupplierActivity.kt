package com.gastrosan.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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



class SupplierActivity : AppCompatActivity() {
    private lateinit var addSupplier: ImageView
    private lateinit var deleteSupplier: ImageView
    private lateinit var gridView: GridView // Agregar esta línea
    private lateinit var buttonDelete: Button
    private lateinit var buttonCancel: Button
    private lateinit var buttonDeleteSupplier: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supplier)

        addSupplier = findViewById(R.id.addSupplier)
        deleteSupplier = findViewById(R.id.deleteSupplier)
        gridView = findViewById(R.id.gridViewInvoices)
        buttonDelete = findViewById(R.id.buttonDelete)
        buttonCancel = findViewById(R.id.buttonCancel)
        buttonDeleteSupplier = findViewById(R.id.buttonDeleteSupplier)

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

        /*//TODO:imageView.setOnClickListener {

            // Obtener la posición del elemento clicado
            val position = position
            expandInvoice(position)
        }*/

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
                    val adapter = InvoiceAdapter(this@SupplierActivity, invoices)
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

    fun expandInvoice(position: Int) {
        //TODO: Implementar la lógica para expandir la factura seleccionada
    }

    private fun loadLogo(imageUrl: String?) {
        val imageViewLogo = findViewById<ImageView>(R.id.imageViewLogo)
        if (!imageUrl.isNullOrEmpty() ) {
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.default_logo)
                    .error(R.drawable.default_logo)
                    .into(imageViewLogo)
            }.addOnFailureListener {
                imageViewLogo.setImageResource(R.drawable.default_logo) // Image load failure
            }
        } else {
            imageViewLogo.setImageResource(R.drawable.default_logo) // Default image if URL is empty or not proper
        }
    }

    data class Invoice(
        val id: String = "", // Proporciona un valor predeterminado para cada parámetro
        val imageUrl: String = "",
        val date: String = "",
        val time: String = "",
        val dateTime: String = ""  // This will be used for sorting

    )

    class InvoiceAdapter(private val context: Context, private var invoices: MutableList<Invoice>) : BaseAdapter() {
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


