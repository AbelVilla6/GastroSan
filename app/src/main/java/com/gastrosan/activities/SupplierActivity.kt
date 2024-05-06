package com.gastrosan.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.gastrosan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class SupplierActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supplier)

        // Recuperar el ID pasado desde DashboardFragment
        val supplierId = intent.getStringExtra("supplierId")
        if (supplierId != null) {
            loadSupplierDetails(supplierId)
        }
    }

    private fun loadSupplierDetails(supplierId: String) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val databaseRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users").child(
            currentUserUid!!
        ).child("suppliers").child(supplierId)

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtener los datos del proveedor desde dataSnapshot y asignarlos a las vistas
                    val supplierName = dataSnapshot.child("name").getValue(String::class.java)
                    val supplierLogoUrl = dataSnapshot.child("logoUrl").getValue(String::class.java)
                    val contactName = dataSnapshot.child("contactName").getValue(String::class.java)
                    val contactPhone = dataSnapshot.child("contactPhone").getValue(String::class.java)
                    val registrationDate = dataSnapshot.child("registrationDate").getValue(String::class.java)

                    val textWithDash = "$contactName:"
                    // Actualizar las vistas con los datos del proveedor
                    val textViewSupplierName = findViewById<TextView>(R.id.textViewSupplierName)
                    textViewSupplierName.text = supplierName
                    val textViewContactName = findViewById<TextView>(R.id.textViewContactName)
                    textViewContactName.text = textWithDash
                    val textViewContactPhone = findViewById<TextView>(R.id.textViewContactPhone)
                    textViewContactPhone.text = contactPhone
                    val textViewRegistrationDate = findViewById<TextView>(R.id.textViewRegistrationDate)
                    textViewRegistrationDate.text = getString(R.string.supplier_since) + " " + registrationDate





                    // Cargar la imagen del logo del proveedor usando Glide
                    val imageViewLogo = findViewById<ImageView>(R.id.imageViewLogo)
                    if (!supplierLogoUrl.isNullOrEmpty()) {
                        if (supplierLogoUrl.startsWith("gs://")) {
                            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(supplierLogoUrl)
                            storageReference.downloadUrl.addOnSuccessListener { uri ->
                                Glide.with(this@SupplierActivity)
                                    .load(uri)
                                    .placeholder(R.drawable.default_logo)
                                    .error(R.drawable.default_logo)
                                    .into(imageViewLogo)
                            }.addOnFailureListener {
                                imageViewLogo.setImageResource(R.drawable.default_logo) // Imagen de fallo
                            }
                        } else {
                            // Carga directa si no es una referencia de Firebase
                            Glide.with(this@SupplierActivity)
                                .load(supplierLogoUrl)
                                .placeholder(R.drawable.default_logo)
                                .error(R.drawable.default_logo)
                                .into(imageViewLogo)
                        }
                    } else {
                        imageViewLogo.setImageResource(R.drawable.default_logo)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores de base de datos
            }
        })
    }

}