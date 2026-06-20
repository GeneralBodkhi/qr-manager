package com.example.qrpay

import android.content.ContentValues
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.OutputStream

class DisplayActivity : AppCompatActivity() {

    private var generatedBitmap: Bitmap? = null
    private var isDecrypted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)

        val tvQrName = findViewById<TextView>(R.id.tvQrName)
        val ivQrCode = findViewById<ImageView>(R.id.ivQrCode)
        val tvQrContentText = findViewById<TextView>(R.id.tvQrContentText)
        val btnToggleMode = findViewById<Button>(R.id.btnToggleMode)
        val btnClose = findViewById<Button>(R.id.btnClose)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val qrName = intent.getStringExtra("QR_NAME") ?: "Без названия"
        val qrContent = intent.getStringExtra("QR_CONTENT") ?: ""

        tvQrName.text = qrName
        tvQrContentText.text = qrContent

        if (qrContent.isNotEmpty()) {
            try {
                val width = 500
                val height = 500

                val bitMatrix = MultiFormatWriter().encode(qrContent, BarcodeFormat.QR_CODE, width, height)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                    }
                }

                generatedBitmap = bitmap
                ivQrCode.setImageBitmap(bitmap)

            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка генерации QR-кода", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "Данные кода пусты", Toast.LENGTH_SHORT).show()
        }

        btnToggleMode.setOnClickListener {
            if (!isDecrypted) {
                ivQrCode.visibility = View.GONE
                tvQrContentText.visibility = View.VISIBLE
                btnToggleMode.text = "Зашифровать"
                btnSave.isEnabled = false
                isDecrypted = true
            } else {
                tvQrContentText.visibility = View.GONE
                ivQrCode.visibility = View.VISIBLE
                btnToggleMode.text = "Расшифровать"
                btnSave.isEnabled = true
                isDecrypted = false
            }
        }

        tvQrContentText.setOnClickListener {
            val textToCopy = tvQrContentText.text.toString()
            if (textToCopy.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("QR Content", textToCopy)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Текст скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener {
            val bitmap = generatedBitmap
            if (bitmap != null) {
                saveBitmapToGallery(bitmap, qrName)
            } else {
                Toast.makeText(this, "Нечего сохранять", Toast.LENGTH_SHORT).show()
            }
        }

        btnClose.setOnClickListener {
            finish()
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap, title: String) {
        val filename = "QR_${title.replace(" ", "_")}_${System.currentTimeMillis()}.png"
        var outputStream: OutputStream? = null

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/QRPay")
            }
        }

        try {
            val resolver = contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (imageUri != null) {
                outputStream = resolver.openOutputStream(imageUri)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream!!)
                Toast.makeText(this, "QR-код сохранен в Галерею!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ошибка создания файла", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось сохранить", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } finally {
            outputStream?.close()
        }
    }
}