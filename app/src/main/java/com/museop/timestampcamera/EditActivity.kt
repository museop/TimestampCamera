package com.museop.timestampcamera

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class EditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        val uri = intent.getParcelableExtra<Uri>("uri")
        uri?.let {
            val bitmap: Bitmap? = getBitmap(uri)
            val imagePreview = findViewById<ImageView>(R.id.imageView)
            imagePreview.setImageBitmap(bitmap)
        }

        val buttonDone = findViewById<Button>(R.id.buttonDone)
        buttonDone.setOnClickListener {
            finishEdit(2)
        }

        val buttonCancel = findViewById<Button>(R.id.buttonCancel)
        buttonCancel.setOnClickListener {
            removeImage(uri)
        }
    }

    private fun finishEdit(returnValue: Int) {
        val resultIntent = Intent()
        resultIntent.putExtra("returnValue", returnValue)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun removeImage(uri: Uri?) {
        if (uri != null) {
            val flag = contentResolver.delete(uri, null, null)
            finishEdit(flag)
        } else {
            finishEdit(0)
        }
    }

    private fun getBitmap(imageUri: Uri): Bitmap? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, imageUri))
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
}