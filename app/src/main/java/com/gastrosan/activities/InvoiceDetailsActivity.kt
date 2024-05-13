package com.gastrosan.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.gastrosan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale
import com.itextpdf.text.*
import com.itextpdf.text.html.WebColors
import com.itextpdf.text.pdf.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date


class InvoiceDetailsActivity : AppCompatActivity() {

    private lateinit var imageViewInvoice: ImageView
    private lateinit var textViewSupplierName: TextView
    private lateinit var textViewDate: TextView
    private lateinit var textViewTotal: TextView
    private lateinit var buttonDeleteInvoice: Button
    private lateinit var buttonAddRow: Button
    private lateinit var emptyMessage: TextView
    private lateinit var deleteRowsButton: Button
    private lateinit var deleteRowsSelectedButton: Button
    private lateinit var buttonSavePdf: Button


    private lateinit var table: TableLayout
    private var invoiceId: String? = null
    private var deleteDialog: AlertDialog? = null

    private val headerTitles = arrayOf("Nombre", "Cantidad", "N° Lote", "PVP", "Importe")
    private val headerWeights = arrayOf(2.75f, 1.75f, 2.0f, 1.5f, 2.0f) // Pesos para el layout de las columnas

    //private var shouldShow: Boolean = false


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
        textViewTotal = findViewById(R.id.textViewTotal)
        textViewDate = findViewById(R.id.textViewDate)
        buttonDeleteInvoice = findViewById(R.id.buttonDeleteInvoice)
        buttonSavePdf = findViewById(R.id.buttonDownloadPdf)
        buttonAddRow = findViewById(R.id.add_row_button)
        table = findViewById(R.id.tableLayout)
        emptyMessage = findViewById<TextView>(R.id.empty_table_message)
        deleteRowsButton = findViewById(R.id.delete_rows_button)
        deleteRowsSelectedButton = findViewById(R.id.delete_rows_selected_button)

        deleteRowsButton.setOnClickListener {
            toggleCheckBoxesVisibility()
        }

        buttonDeleteInvoice.setOnClickListener {
            deleteInvoice()
        }
        imageViewInvoice.setOnClickListener {
            showFullImageDialog(intent.getStringExtra("imageUrl"))
        }
        buttonAddRow.setOnClickListener {
            showAddDataRowDialog()
        }
        buttonSavePdf.setOnClickListener {
            createPdf()
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

        deleteRowsSelectedButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }
    private fun showDeleteConfirmationDialog() {
        val selectedRowsCount = getSelectedRowsCount()
        if (selectedRowsCount > 0) {
            AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar $selectedRowsCount fila(s)?")
                .setPositiveButton("Sí") { _, _ ->
                    deleteSelectedRows()
                }
                .setNegativeButton("No", null)
                .show()
        } else {
            Toast.makeText(this, "Por favor, selecciona al menos una fila para eliminar.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSelectedRowsCount(): Int {
        var selectedCount = 0
        for (i in 1 until table.childCount) {
            val row = table.getChildAt(i) as TableRow
            val child = row.getChildAt(0)
            if (child is CheckBox) {
                val checkBox = child
                if (checkBox.isChecked) {
                selectedCount++
                }
            }
            else {
                println("Unexpected child type at row $i: ${child.javaClass.simpleName}")
            }
        }
        return selectedCount
    }

    private fun deleteSelectedRows() {
        var i = 1
        while (i < table.childCount) {
            val row = table.getChildAt(i) as TableRow
            val checkBox = row.getChildAt(0) as CheckBox
            if (checkBox.isChecked) {
                table.removeViewAt(i)
            } else {
                i++
            }
        }
        resetTableState()

        updateDeleteButtonState()
        updateEmptyMessageAndTotal()
        //shouldShow = true
        //buttonAddRow.visibility = View.VISIBLE
    }
    private fun resetTableState() {
        // Eliminar el checkbox del encabezado si está presente
        val headerRow = table.getChildAt(0) as TableRow
        if (headerRow.childCount > headerTitles.size) {
            headerRow.removeViewAt(0)
        }

        // Eliminar checkboxes de todas las filas
        for (i in 1 until table.childCount) {
            val row = table.getChildAt(i) as TableRow
            if (row.childCount > headerTitles.size) {
                row.removeViewAt(0)
            }
        }

        // Restaurar visibilidad de botones
        deleteRowsSelectedButton.visibility = View.GONE
        deleteRowsButton.visibility = View.VISIBLE
        buttonAddRow.visibility = View.VISIBLE
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

        table.addView(createHeaderRow(headerTitles, headerWeights))

        // Añadir filas de ejemplo
        for (i in 1..12) { // Cambiar por datos reales
            addRowToTable("Itemsssssssssssssssss $i", i, "Lot $i", "100", (100 * i).toString(), false)
        }

        updateEmptyMessageAndTotal()
        updateDeleteButtonState()
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
                    addRowToTable(name, null, null, null, formatPrice(importe), isExtraCost = true)
                    updateEmptyMessageAndTotal()
                    dialog.dismiss()
                }
                else {
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


    private fun addRowToTable(name: String, quantity: Int?, lot: String?, pvp: String?, importe: String, isExtraCost: Boolean = false, includeCheckbox: Boolean = false) {
        val row = TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
            background = if (isExtraCost) ContextCompat.getDrawable(this@InvoiceDetailsActivity, R.drawable.table_cell_extra_cost_background)
            else ContextCompat.getDrawable(this@InvoiceDetailsActivity, R.drawable.table_cell_background)
        }

        if (includeCheckbox) {
            val checkBox = CheckBox(this)
            checkBox.visibility = View.VISIBLE
            // Aquí asignamos un peso muy pequeño para hacer la celda del checkbox más estrecha
            val checkBoxParams = TableRow.LayoutParams(dpToPx(5), TableRow.LayoutParams.MATCH_PARENT)
            row.addView(checkBox, checkBoxParams)
        }

        // Asegúrate de que la suma de los pesos de todas las celdas más el peso del checkbox sea coherente
        row.addView(createCell(getValidText(name), 2.74f, isExtraCost))
        row.addView(createCell(getValidText(quantity?.toString()), 1.75f, isExtraCost))
        row.addView(createCell(getValidText(lot), 2.0f, isExtraCost))
        row.addView(createCell(getValidText(pvp), 1.5f, isExtraCost))
        row.addView(createCell(importe, 2.0f, isExtraCost))

        table.addView(row)
        updateDeleteButtonState()
    }



    private fun getValidText(input: String?): String {
        return if (input.isNullOrEmpty()) "-" else input
    }

    private fun updateDeleteButtonState() {
        deleteRowsButton.visibility = if (table.childCount > 1) View.VISIBLE else View.GONE
    }

    private fun toggleCheckBoxesVisibility() {
        val shouldShow = deleteRowsButton.visibility == View.VISIBLE
        buttonAddRow.visibility = if (shouldShow) View.GONE else View.VISIBLE

        // Ajustar el encabezado para agregar o quitar el checkbox
        val headerRow = table.getChildAt(0) as TableRow
        if (shouldShow && headerRow.childCount == headerTitles.size) {
            headerRow.addView(createHeaderCheckboxCell(), 0)
        } else if (!shouldShow && headerRow.childCount > headerTitles.size) {
            headerRow.removeViewAt(0)
        }

        // Ajustar las filas de datos para agregar o quitar checkboxes
        for (i in 1 until table.childCount) {
            val row = table.getChildAt(i) as TableRow
            if (shouldShow && row.childCount == headerTitles.size) {
                val checkBox = CheckBox(this)
                checkBox.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)
                checkBox.gravity = Gravity.CENTER
                row.addView(checkBox, 0)
            } else if (!shouldShow && row.childCount > headerTitles.size) {
                row.removeViewAt(0)
            }
        }

        deleteRowsButton.visibility = if (shouldShow) View.GONE else View.VISIBLE
        deleteRowsSelectedButton.visibility = if (shouldShow) View.VISIBLE else View.GONE
        updateEmptyMessageAndTotal()
    }
    private fun createHeaderCheckboxCell(): TextView {
        return TextView(this).apply {
            text = ""  // Celda vacía para alinear con los CheckBoxes
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(context, R.drawable.table_cell_black_background)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
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
        val format = NumberFormat.getNumberInstance(Locale.GERMANY) // Ajusta el formato según tus necesidades
        var total = 0.0
        for (i in 1 until table.childCount) {
            val row = table.getChildAt(i) as TableRow
            val scrollView = row.getChildAt(4) as HorizontalScrollView // Acceder al HorizontalScrollView que ahora contiene el TextView
            val frameLayout = scrollView.getChildAt(0) as FrameLayout
            val importeCell = frameLayout.getChildAt(0) as TextView
            val importeText = importeCell.text.toString()
            try {
                val importe = format.parse(importeText)?.toDouble() ?: 0.0
                total += importe
            } catch (e: ParseException) {
                println("Failed to parse importe '$importeText' at row $i: ${e.message}")
            }
        }
        findViewById<TextView>(R.id.textViewTotal).text = "Total: ${String.format("%.2f", total)}€"
    }


    private fun createHeaderRow(titles: Array<String>, weights: Array<Float>, includeCheckbox: Boolean = false): TableRow {
        val headerRow = TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
        }

        // Celda del checkbox en el encabezado
        if (includeCheckbox) {
            headerRow.addView(TextView(this).apply {
                text = ""
                // Ajuste del ancho de la celda del checkbox, usando un peso menor
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 0.0001f) // Ajustar según necesidades
                gravity = Gravity.CENTER
                background = ContextCompat.getDrawable(context, R.drawable.table_cell_background)
            })
        }

        // Celdas del encabezado
        titles.forEachIndexed { index, title ->
            headerRow.addView(TextView(this).apply {
                text = title
                setTextAppearance(this@InvoiceDetailsActivity, R.style.TableHeader)
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, weights[index])
                gravity = Gravity.CENTER
                background = ContextCompat.getDrawable(context, R.drawable.table_cell_background)
            })
        }

        return headerRow
    }


    private fun createCell(text: String, weight: Float, isExtraCost: Boolean = false): View {
        // Crear un TextView para el contenido
        val textView = TextView(this).apply {
            setTextAppearance(this@InvoiceDetailsActivity, R.style.TableCell)
            this.text = text
            gravity = Gravity.CENTER // Centra el texto horizontal y verticalmente
            setPadding(8, 8, 8, 8)
            ellipsize = TextUtils.TruncateAt.END // Configura cómo el texto es truncado
            maxLines = 1 // Una sola línea, se desplazará horizontalmente si es necesario
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, // Ancho de contenido
                FrameLayout.LayoutParams.MATCH_PARENT) // Altura para igualar la del ScrollView
        }

        // Envolver el TextView en un FrameLayout para centrar fácilmente el contenido
        val frameLayout = FrameLayout(this).apply {
            addView(textView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, // Ancho de contenido
                FrameLayout.LayoutParams.MATCH_PARENT, // Altura para igualar la del ScrollView
                Gravity.CENTER)) // Centra el TextView dentro del FrameLayout
        }

        // Crear un HorizontalScrollView y añadir el FrameLayout a este
        val scrollView = HorizontalScrollView(this).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, weight)
            addView(frameLayout, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, // Ancho de contenido
                ViewGroup.LayoutParams.MATCH_PARENT)) // Altura para igualar la del ScrollView
            isHorizontalScrollBarEnabled = false // Deshabilita la barra de desplazamiento visual, pero permite el desplazamiento
        }

        // Configura el fondo dependiendo del tipo de costo
        scrollView.background = if (isExtraCost)
            ContextCompat.getDrawable(this@InvoiceDetailsActivity, R.drawable.table_cell_extra_cost_background)
        else
            ContextCompat.getDrawable(this@InvoiceDetailsActivity, R.drawable.table_cell_background)

        return scrollView
    }

    private fun createPdf() {
        val doc = Document()
        try {
            val resolver = contentResolver
            val formattedSupplierName = textViewSupplierName.text.toString().replace(" ", "_").replace("/", "_")
            val formattedDate = textViewDate.text.toString().replace(" ", "_").replace("/", "_")
            val fileName = "factura_${formattedSupplierName}_$formattedDate.pdf"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            uri?.let {
                resolver.openOutputStream(it).use { outputStream ->
                    val writer = PdfWriter.getInstance(doc, outputStream)
                    doc.open()

                    // Encabezado con logo y color personalizado
                    val header = PdfPTable(1).apply {
                        widthPercentage = 100f
                        val cell = PdfPCell().apply {
                            val primaryColor = WebColors.getRGBColor("#302B63") // Reemplaza con tu código de color
                            backgroundColor = BaseColor(primaryColor.red, primaryColor.green, primaryColor.blue)
                            border = Rectangle.NO_BORDER
                            val logo = Image.getInstance(drawableToByteArray(R.drawable.logo_gastrosan))
                            logo.scaleToFit(80f, 80f)
                            logo.alignment = Image.ALIGN_CENTER
                            addElement(logo)
                        }
                        addCell(cell)
                    }
                    doc.add(header)

                    // Agregar detalles del proveedor y fecha con texto adicional
                    doc.add(Paragraph("Distribuidor: ${textViewSupplierName.text.toString()}").apply {
                        alignment = Element.ALIGN_CENTER
                    })
                    doc.add(Paragraph("Factura subida a GastroSan el ${textViewDate.text.toString()}").apply {
                        alignment = Element.ALIGN_CENTER
                    })

                    // Espacio antes de la imagen de la factura
                    doc.add(Chunk.NEWLINE)

                    // Agregar la imagen de la factura centrada y más grande
                    val invoiceImage = Image.getInstance(imageViewInvoice.drawable.toBitmap().toByteArray())
                    invoiceImage.scaleToFit(300f, 200f) // Ajusta estos valores según tus necesidades
                    invoiceImage.alignment = Image.ALIGN_CENTER
                    doc.add(invoiceImage)

                    // Espacio antes de la tabla
                    doc.add(Chunk.NEWLINE)

                    // Agregar tabla
                    val pdfTable = PdfPTable(5)
                    val cellHeaders = arrayOf("Artículo", "Cantidad", "Nº Lote", "PVP", "Importe")
                    cellHeaders.forEach { header ->
                        pdfTable.addCell(header)
                    }

                    val count = table.childCount
                    for (i in 1 until count) { // Comenzar en 1 para saltar el encabezado
                        val row = table.getChildAt(i) as TableRow
                        for (j in 0 until row.childCount) {
                            val cellView = row.getChildAt(j)
                            if (cellView is HorizontalScrollView) {
                                val innerFrameLayout = cellView.getChildAt(0) as FrameLayout
                                val textView = innerFrameLayout.getChildAt(0) as TextView
                                pdfTable.addCell(textView.text.toString())
                            } else if (cellView is TextView) {
                                pdfTable.addCell((cellView as TextView).text.toString())
                            }
                        }
                    }
                    doc.add(pdfTable)

                    // Total
                    if (textViewTotal.visibility == View.VISIBLE) {
                        doc.add(Paragraph("${textViewTotal.text.toString()}").apply {
                            font.size = 16f
                            font.style = Font.BOLD
                            alignment = Element.ALIGN_CENTER
                        })
                    }

                    writer.pageEvent = FooterEvent()


                    doc.close()
                    Toast.makeText(this, "PDF guardado en Downloads", Toast.LENGTH_LONG).show()
                }
            } ?: throw IOException("No se pudo crear el archivo PDF")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al crear PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable) {
            return this.bitmap
        }
        val bitmap = Bitmap.createBitmap(this.intrinsicWidth, this.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.setBounds(0, 0, canvas.width, canvas.height)
        this.draw(canvas)
        return bitmap
    }

    private fun drawableToByteArray(drawableId: Int): ByteArray {
        val drawable = ContextCompat.getDrawable(this, drawableId) as BitmapDrawable
        val bitmap = drawable.bitmap
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    class FooterEvent : PdfPageEventHelper() {
        override fun onEndPage(writer: PdfWriter, document: Document) {
            val footerText = "Todos los Derechos Reservados - GastroSan ©, ${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())}"
            val footer = Paragraph(footerText)
                .apply {
                    alignment = Element.ALIGN_RIGHT
                    font.color = BaseColor.GRAY
                    font.style = Font.ITALIC
                    font.size = 8f}
            ColumnText.showTextAligned(writer.directContent, Element.ALIGN_RIGHT, footer, document.right(), document.bottom() - 10f, 0f)
        }
    }





}