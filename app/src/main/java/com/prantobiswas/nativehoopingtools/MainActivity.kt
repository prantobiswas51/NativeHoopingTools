package com.prantobiswas.nativehoopingtools


import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private var originalBitmap: Bitmap? = null
    private lateinit var chooseImageButton: Button
    private lateinit var saveImage: Button
    private lateinit var selectedImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chooseImageButton = findViewById(R.id.chooseImageButton)
        saveImage = findViewById(R.id.saveImage)
        selectedImage = findViewById(R.id.selectedImage)


        // Set up button click listeners
        chooseImageButton.setOnClickListener { openImagePicker() }
        saveImage.setOnClickListener { saveWatermarkedImage() }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            try {
                // Load the selected image
                originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                selectedImage.setImageBitmap(originalBitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun saveWatermarkedImage() {
        originalBitmap?.let { original ->
            // Create a new bitmap with the watermark
            val watermarkedBitmap = addWatermark(original)

            // Save the watermarked image to the gallery
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "watermarked_${UUID.randomUUID()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }

            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    showToast("Image saved successfully")
                }
            }
        }
    }

    private fun addWatermark(originalBitmap: Bitmap): Bitmap {
        val watermarkedBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, originalBitmap.config)
        val canvas = Canvas(watermarkedBitmap)

        // Draw the original image
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        // Calculate the text size based on a percentage of the image dimensions
        val textSizePercentage = 10 // You can adjust this percentage as needed
        val baseTextSize = minOf(originalBitmap.width, originalBitmap.height) * textSizePercentage / 100f

        // Draw the watermark
        val watermarkText = "Native Hooping"
        val watermarkPaint = Paint().apply {
            color = Color.argb(77, 255, 255, 255)
            textSize = baseTextSize
            isAntiAlias = true
            setShadowLayer(1f, 0.5f, 0.5f, Color.argb(77, 255, 255, 255))
        }

        // Center the text on the image
        val x = (originalBitmap.width - watermarkPaint.measureText(watermarkText)) / 2
        val y = (originalBitmap.height - watermarkPaint.descent() - watermarkPaint.ascent()) / 2

        canvas.drawText(watermarkText, x, y, watermarkPaint)

        return watermarkedBitmap
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
