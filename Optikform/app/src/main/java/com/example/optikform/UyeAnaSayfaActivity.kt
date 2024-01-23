package com.example.optikform

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class UyeAnaSayfaActivity : AppCompatActivity() {

    private lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uye_anasayfa)


        storageReference = FirebaseStorage.getInstance().reference

        val btnCevapAnahtari: Button = findViewById(R.id.btnCevapAnahtari)
        val btnKontrolYap: Button = findViewById(R.id.btnKontrolYap)
        val btnOptikFormIndir: Button = findViewById(R.id.btnOptikFormIndir)


        btnCevapAnahtari.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val intent = Intent(this@UyeAnaSayfaActivity, CevapAnahtariOlusturActivity::class.java)
                startActivity(intent)
            }
        })

        btnKontrolYap.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val intent = Intent(this@UyeAnaSayfaActivity, KontrolYapActivity::class.java)
                startActivity(intent)
            }
        })

        btnOptikFormIndir.setOnClickListener {

            downloadImageFromFirebaseStorage("optikform.png")
        }


    }


    private fun downloadImageFromFirebaseStorage(imageName: String) {
        val localFile = File.createTempFile("images", "jpg")

        storageReference.child(imageName).getFile(localFile)
            .addOnSuccessListener {

                saveImageToGallery(localFile)
                Toast.makeText(this@UyeAnaSayfaActivity, "Resim başarıyla indirildi ve kaydedildi", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {

                Toast.makeText(this@UyeAnaSayfaActivity,  "Resim indirilemedi", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveImageToGallery(imageFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "image.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            uri?.let {
                contentResolver.openOutputStream(it).use { outputStream ->
                    outputStream?.let {
                        saveBitmapToStream(BitmapFactory.decodeFile(imageFile.absolutePath), it)
                        Toast.makeText(this@UyeAnaSayfaActivity, "Resim galeriye kaydedildi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, "image.jpg")

            saveBitmapToStream(BitmapFactory.decodeFile(imageFile.absolutePath), FileOutputStream(image))

            Toast.makeText(this@UyeAnaSayfaActivity, "Resim galeriye kaydedildi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToStream(bitmap: Bitmap, outputStream: OutputStream) {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
    }
}