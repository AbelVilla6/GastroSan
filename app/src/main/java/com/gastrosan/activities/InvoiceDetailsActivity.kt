package com.gastrosan.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.gastrosan.R
import android.app.Dialog
import android.content.Intent
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale

class InvoiceDetailsActivity : AppCompatActivity() {

    private lateinit var imageViewInvoice: ImageView
    private lateinit var textViewSupplierName: TextView
    private lateinit var textViewDate: TextView
    private lateinit var buttonDeleteInvoice: Button
    private lateinit var buttonAddRow: Button
    private lateinit var emptyMessage: TextView
    private lateinit var table: TableLayout
    private var invoiceId: String? = null
    private var deleteDialog: AlertDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice_details)
        initializeViews()
        setupData()
        populateTable()
    }

    private fun initializeViews() {
        imageViewInvoice = findViewById(R.id.imageViewInvoice)
        textViewSupplierName = findViewById(R.id.textViewSupplierName)
        textViewDate = findViewById(R.id.textViewDate)
        buttonDeleteInvoice = findViewById(R.id.buttonDeleteInvoice)
        buttonAddRow = findViewById(R.id.add_row_button)
        table = findViewById(R.id.tableLayout)
        emptyMessage = findViewById<TextView>(R.id.empty_table_message)

        buttonDeleteInvoice.setOnClickListener {
            deleteInvoice()
        }
        imageViewInvoice.setOnClickListener {
            showFullImageDialog(intent.getStringExtra("imageUrl"))
        }
        buttonAddRow.setOnClickListener {
            showAddDataRowDialog()
        }
    }

    private fun setupData() {
        val imageUrl = intent.getStringExtra("imageUrl")
        val supplierName = intent.getStringExtra("supplierName")
        val date = intent.getStringExtra("date")
        val time = intent.getStringExtra("time")
        invoiceId = intent.getStringExtra("invoiceId")

        Glide.with(this).load(imageUrl).into(imageViewInvoice)
        textViewSupplierName.text = supplierName
        textViewDate.text = "$date a las $time"
    }


    private fun deleteInvoice() {
        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
            val userUid = currentUser.uid
            val supplierId = intent.getStringExtra("supplierId") ?: return
            AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar esta factura?")
                .setPositiveButton("Eliminar") { _, _ ->
                    val invoiceRef = FirebaseDatabase.getInstance("https://gastrosan-app-default-rtdb.europe-west1.firebasedatabase.app/")
                        .getReference("users/$userUid/suppliers/$supplierId/invoices/$invoiceId")
                    invoiceRef.removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Factura eliminada correctamente", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Error al eliminar la factura", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } ?: run {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        deleteDialog?.dismiss()  // Dismiss the dialog when the activity is not in the foreground
    }

    private fun showFullImageDialog(imageUrl: String?) {
        Dialog(this, R.style.FullScreenDialog).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
            setContentView(R.layout.dialog_full_image2)
            findViewById<it.sephiroth.android.library.imagezoom.ImageViewTouch>(R.id.fullImageView).also { imageView ->
                Glide.with(this@InvoiceDetailsActivity).load(imageUrl).into(imageView)
            }
            findViewById<ImageView>(R.id.closeButton).setOnClickListener { dismiss() }
            show()
        }
    }

    private fun populateTable() {
        val table = findViewById<TableLayout>(R.id.tableLayout)
        table.removeAllViews()

        val headerTitles = arrayOf("Nombre", "Cantidad", "N° Lote", "PVP", "Importe")
        val headerWeights = arrayOf(2.75f, 1.75f, 2.0f, 1.5f, 2.0f)
        table.addView(createHeaderRow(headerTitles, headerWeights))

        updateEmptyMessageAndTotal()

        table.requestLayout()  // Solicitar una nueva disposición después de modificar la tabla

    }
    private fun showAddDataRowDialog() {
        AlertDialog.Builder(this)
            .setTitle("¿Qué deseas añadir?")
            .setNegativeButton("Cancelar", null)
            .setItems(arrayOf("Artículos", "Costes Extras (IVA, Otros gastos...)")) { dialog, which ->
                when (which) {
                    0 -> showArticleDialog()
                    1 -> showExtraCostDialog()
                }
            }.show()
    }

    private fun showArticleDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_data, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.input_name)
        val quantityInput = dialogView.findViewById<EditText>(R.id.input_quantity)
        val lotInput = dialogView.findViewById<EditText>(R.id.input_lote)
        val pvpInput = dialogView.findViewById<EditText>(R.id.input_pvp)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Añadir", null) // Set to null. We override the onClick below.
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val name = formatName(nameInput.text.toString())
                val quantity = quantityInput.text.toString().toIntOrNull()
                val pvp = normalizePvpInput(pvpInput.text.toString())

                var isValid = true
                if (name.isBlank()) {
                    nameInput.error = "Campo obligatorio"
                    isValid = false
                }
                if (quantity == null || quantity <= 0) {
                    quantityInput.error = "Cantidad inválida"
                    isValid = false
                }
                if (pvp.isBlank()) {
                    pvpInput.error = "Campo obligatorio"
                    isValid = false
                }

                if (isValid) {
                    val pvpDouble = parseGermanNumber(pvp)
                    val importe = calculateImporte(quantity!!, pvpDouble)
                    addRowToTable(name, quantity, lotInput.text.toString(), formatPrice(pvpDouble), formatPrice(importe))
                    updateEmptyMessageAndTotal()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Por favor, corrige los errores antes de añadir.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }


    @SuppressLint("MissingInflatedId")
    private fun showExtraCostDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_extra_cost, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.input_name)
        val importInput = dialogView.findViewById<EditText>(R.id.input_importe)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Añadir", null) // Set to null. We override the onClick below.
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val name = formatName(nameInput.text.toString())
                val importeText = normalizePvpInput(importInput.text.toString())
                var isValid = true

                // Validaciones
                if (name.isBlank()) {
                    nameInput.error = "Campo obligatorio"
                    isValid = false
                }
                if (importeText.isBlank()) {
                    importInput.error = "Campo obligatorio"
                    isValid = false
                }

                val importe = try {
                    NumberFormat.getNumberInstance(Locale.GERMANY).parse(importeText)?.toDouble() ?: 0.0
                } catch (e: ParseException) {
                    importInput.error = "Formato de número inválido"
                    0.0
                }

                if (importe <= 0) {
                    importInput.error = "El importe debe ser mayor que 0"
                    isValid = false
                }

                if (isValid) {
                    addRowToTable(name, 0, "", "", formatPrice(importe), isExtraCost = true)
                    updateEmptyMessageAndTotal()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Por favor, corrige los errores antes de continuar.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }



    private fun normalizePvpInput(input: String): String {
        // Replace comma with dot and ensure only one decimal place
        return input.replace(',', '.')
    }

    private fun parseGermanNumber(number: String): Double {
        try {
            // Parse the number assuming it is in German format
            return NumberFormat.getNumberInstance(Locale.GERMANY).parse(number)?.toDouble() ?: 0.0
        } catch (e: ParseException) {
            Toast.makeText(this, "Error parsing number format.", Toast.LENGTH_SHORT).show()
            return 0.0
        }
    }
    private fun formatName(name: String): String {
        return name.split(" ").joinToString(" ") { it.capitalize() }
    }

    private fun formatPrice(price: Double): String {
        return String.format(Locale.GERMANY, "%.2f", price)
    }

    private fun calculateImporte(quantity: Int?, pvp: Double): Double {
        return quantity?.times(pvp) ?: 0.0
    }

    private fun addRowToTable(name: String, quantity: Int, lot: String, pvp: String, importe: String, isExtraCost: Boolean = false) {
        val row = TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
            if (isExtraCost) {
                background = ContextCompat.getDrawable(this@InvoiceDetailsActivity, R.drawable.table_cell_extra_cost_background)
            } else {
                background = ContextCompat.getDrawable(this@InvoiceDetailsActivity, R.drawable.table_cell_background)
            }
        }

        val cells = listOf(
            createCell(name, 2.75f, isExtraCost),
            createCell(if (isExtraCost) "-" else quantity.toString(), 1.75f, isExtraCost),
            createCell(if (isExtraCost) "-" else lot, 2.0f, isExtraCost),
            createCell(if (isExtraCost) "-" else pvp, 1.5f, isExtraCost),
            createCell(importe, 2.0f, isExtraCost)
        )

        cells.forEach { cell -> row.addView(cell) }
        table.addView(row)

        updateEmptyMessageAndTotal()
    }

    private fun updateEmptyMessageAndTotal() {
        // Se asegura que la tabla tenga solo la fila de encabezado cuando no hay datos.
        if (table.childCount == 1) { // Supone que solo la fila de encabezado está presente.
            emptyMessage.visibility = View.VISIBLE
            findViewById<TextView>(R.id.textViewTotal).visibility = View.GONE
        } else {
            emptyMessage.visibility = View.GONE
            findViewById<TextView>(R.id.textViewTotal).visibility = View.VISIBLE
            updateTotal()
        }
    }
    private fun updateTotal() {
        val format = NumberFormat.getNumberInstance(Locale.GERMANY) // Adjust locale to fit your needs
        var total = 0.0
        for (i in 1 until table.childCount) {
            val row = table.getChildAt(i) as TableRow
            val importeCell = row.getChildAt(4) as TextView
            val importeText = importeCell.text.toString()
            try {
                val importe = format.parse(importeText)?.toDouble() ?: 0.0
                total += importe
                println("Parsed Importe at row $i: $importe")
            } catch (e: Exception) {
                println("Failed to parse importe '$importeText' at row $i: ${e.message}")
            }
        }
        findViewById<TextView>(R.id.textViewTotal).text = "Total: ${String.format("%.2f", total)}€"
    }



    private fun createHeaderRow(titles: Array<String>, weights: Array<Float>): TableRow =
        TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
            titles.forEachIndexed { index, title ->
                addView(TextView(this@InvoiceDetailsActivity).apply {
                    text = title
                    setTextAppearance(this@InvoiceDetailsActivity, R.style.TableHeader)
                    layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weights[index])
                    gravity = Gravity.CENTER
                    background = ContextCompat.getDrawable(context, R.drawable.table_cell_background)
                })
            }
        }

    private fun createCell(text: String, weight: Float, isExtraCost: Boolean = false): TextView {
        return TextView(this).apply {
            setTextAppearance(this@InvoiceDetailsActivity, R.style.TableCell)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, weight)
            this.text = text
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            background = if (isExtraCost)
                ContextCompat.getDrawable(this@InvoiceDetailsActivity, R.drawable.table_cell_extra_cost_background)
            else
                ContextCompat.getDrawable(this@InvoiceDetailsActivity, R.drawable.table_cell_background)
        }
    }


}