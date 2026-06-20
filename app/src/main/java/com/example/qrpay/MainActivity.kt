package com.example.qrpay

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var lvQrList: ListView
    private lateinit var btnScan: Button
    private lateinit var btnGallery: Button
    private lateinit var adapter: ArrayAdapter<String>

    private lateinit var db: AppDatabase
    private var qrListFromDb = ArrayList<QrCodeEntity>()
    private val displayNames = ArrayList<String>()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scanImageFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        lvQrList = findViewById(R.id.lvQrList)
        btnScan = findViewById(R.id.btnScan)
        btnGallery = findViewById(R.id.btnGallery)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayNames)
        lvQrList.adapter = adapter

        refreshDataFromDatabase()

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .allowManualInput()
            .build()

        val scanner = GmsBarcodeScanning.getClient(this, options)

        btnScan.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener { barcode: Barcode ->
                    val rawValue: String? = barcode.rawValue
                    if (rawValue != null) {
                        showNameInputDialog(rawValue)
                    }
                }
                .addOnFailureListener { e: Exception ->
                    Toast.makeText(this, "Ошибка: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
                .addOnCanceledListener {
                    Toast.makeText(this, "Сканирование отменено", Toast.LENGTH_SHORT).show()
                }
        }

        btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        lvQrList.setOnItemClickListener { _, _, position, _ ->
            if (position < qrListFromDb.size) {
                val clickedQr = qrListFromDb[position]
                val intent = android.content.Intent(this, DisplayActivity::class.java)
                intent.putExtra("QR_NAME", clickedQr.name)
                intent.putExtra("QR_CONTENT", clickedQr.content)
                startActivity(intent)
            }
        }

        lvQrList.setOnItemLongClickListener { _, _, position, _ ->
            if (position < qrListFromDb.size) {
                val clickedQr = qrListFromDb[position]

                AlertDialog.Builder(this)
                    .setTitle("Удаление кода")
                    .setMessage("Вы уверены, что хотите удалить \"${clickedQr.name}\"?")
                    .setPositiveButton("Удалить") { dialog, _ ->
                        thread {
                            db.qrCodeDao().deleteCode(clickedQr)
                            runOnUiThread { refreshDataFromDatabase() }
                        }
                        Toast.makeText(this, "Код удален", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
            true
        }
    }

    private fun scanImageFromUri(uri: android.net.Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val localScanner = BarcodeScanning.getClient()

            localScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val qrContent = barcodes[0].rawValue
                        if (qrContent != null) {
                            showNameInputDialog(qrContent)
                        } else {
                            Toast.makeText(this, "QR-код пуст", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "QR-код на картинке не обнаружен", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка чтения файла: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось загрузить изображение", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshDataFromDatabase() {
        thread {
            val codes = db.qrCodeDao().getAllCodes()
            runOnUiThread {
                qrListFromDb.clear()
                qrListFromDb.addAll(codes)

                displayNames.clear()
                for (qr in qrListFromDb) {
                    displayNames.add(qr.name)
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun showNameInputDialog(qrContent: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Код успешно отсканирован!")
        builder.setMessage("Введите название для этого QR-кода:")

        val input = EditText(this)
        input.hint = "Например: Ключ от сейфа"
        builder.setView(input)

        builder.setPositiveButton("Сохранить") { dialog, _ ->
            var name = input.text.toString().trim()
            if (name.isEmpty()) {
                name = "Безымянный код (${qrListFromDb.size + 1})"
            }

            val newIdCard = QrCodeEntity(name = name, content = qrContent)

            thread {
                db.qrCodeDao().insertCode(newIdCard)
                runOnUiThread { refreshDataFromDatabase() }
            }

            Toast.makeText(this, "Сохранено: $name", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ -> dialog.cancel() }
        builder.show()
    }
}