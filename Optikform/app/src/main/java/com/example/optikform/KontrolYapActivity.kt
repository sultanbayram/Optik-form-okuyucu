package com.example.optikform
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import kotlin.math.min


class KontrolYapActivity : AppCompatActivity() {
    private var textTarama = false
    private var imageDosyasi: File? = null
    private var taramaSonucu = ""
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var dersSecSpinner: Spinner
    private lateinit var kontrolSonucTextView: TextView
    private lateinit var taramaSonucuTextView: TextView
    private lateinit var karsilastirButton: Button
    private var satirSayisi = 0
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val inputStream = contentResolver.openInputStream(it)
                val orijinalBitmap = BitmapFactory.decodeStream(inputStream)
                val siyahBeyazBitmap = siyahBeyazDonustur(orijinalBitmap)

                val geciciDosya = File.createTempFile("temp", null, cacheDir)
                geciciDosya.outputStream().use { outputStream ->
                    siyahBeyazBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }

                imageDosyasi = geciciDosya
                findViewById<ImageView>(R.id.imageView).setImageBitmap(siyahBeyazBitmap)
                textTarama = true
                taninanMetniAl()
            }
        }

    private val resimCek =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { basarili ->
            if (basarili) {
                val imageBitmap = BitmapFactory.decodeFile(imageDosyasi!!.absolutePath)
                val siyahBeyazBitmap = siyahBeyazDonustur(imageBitmap)

                findViewById<ImageView>(R.id.imageView).setImageBitmap(siyahBeyazBitmap)
                textTarama = true
                taninanMetniAl()
            }
        }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kontrol_yap)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()
        dersSecSpinner = findViewById(R.id.spinnerDersSec)
        kontrolSonucTextView = findViewById(R.id.textViewKontrolSonuc)
        taramaSonucuTextView = findViewById(R.id.tarananMetinSonucu)

        findViewById<Button>(R.id.btnGallery).setOnClickListener {
            izinKontroluVeGaleriAc()
        }

        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            izinKontroluVeKameraAc()
        }
        karsilastirButton = findViewById(R.id.buttonKarsilastir)

        karsilastirButton.setOnClickListener {
            karsilastirButtonOnClick()
        }

        fetchDersAdlari()


        dersSecSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()

                if (selectedItem != "Ders Seç") {
                    fetchScannedText(selectedItem)
                } else {
                    // "Ders Seç" seçildiğinde yapılacak işlemler
                    kontrolSonucTextView.text = "Lütfen bir ders seçin."
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        })


    }

    private fun karsilastirButtonOnClick() {
        val scannedTextFromFirebase = kontrolSonucTextView.text.toString()
        val scannedTextFromImage = taramaSonucuTextView.text.toString()

        val firebaseLines = scannedTextFromFirebase.split("\n")
        val imageLines = scannedTextFromImage.split("\n")
        val startIndex = maxOf(
            firebaseLines.indexOfFirst { it.startsWith("01.") },
            imageLines.indexOfFirst { it.startsWith("01.") }
        )

        val totalLines = min(firebaseLines.size - startIndex, imageLines.size - startIndex)
        var matchingLines = 0

        for (i in 0 until totalLines) {
            val firebaseLine = firebaseLines[startIndex + i]
            val imageLine = imageLines[startIndex + i]

            if (firebaseLine == imageLine) {
                matchingLines++
            }
        }

        val accuracy = (matchingLines.toDouble() / totalLines) * 100
        findViewById<TextView>(R.id.benzerlikPuaniTextView).text = "Puan: $accuracy%"
    }




    private fun fetchDersAdlari() {
        val userUid = firebaseAuth.currentUser?.uid
        userUid?.let {
            firebaseFirestore.collection("cevapAnahtari")
                .document(it)
                .collection("kayitlar")
                .get()
                .addOnSuccessListener { result ->
                    val dersAdlari = mutableListOf<String>()
                    dersAdlari.add("Ders Seç") // Başlangıç öğesi

                    for (document in result.documents) {
                        val dersAdi = document.getString("dersAdi")
                        dersAdi?.let {
                            dersAdlari.add(it)
                        }
                    }

                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dersAdlari)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    dersSecSpinner.adapter = adapter
                }
                .addOnFailureListener { e ->
                    Log.w("Firebase", "Ders Adları Getirme Hatası", e)
                }
        }
    }
    private fun fetchScannedText(selectedDersAdi: String) {
        val userUid = firebaseAuth.currentUser?.uid
        userUid?.let {
            firebaseFirestore.collection("cevapAnahtari")
                .document(it)
                .collection("kayitlar")
                .whereEqualTo("dersAdi", selectedDersAdi)
                .get()
                .addOnSuccessListener { result ->
                    if (result.documents.isNotEmpty()) {

                        val taramaSonucu = result.documents[0].getString("taramaSonucu")
                        kontrolSonucTextView.text = taramaSonucu


                        val satirSayisi = result.documents[0].getLong("satirSayisi")
                        this.satirSayisi = satirSayisi?.toInt() ?: 0


                        arayuzGuncelle()
                    } else {
                        kontrolSonucTextView.text = "Seçilen ders adına ait kayıt bulunamadı."
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Firebase", "Cevap Anahtarı Getirme Hatası", e)
                    Toast.makeText(this, "Bir hata oluştu. Lütfen tekrar deneyin.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun arayuzGuncelle() {
        runOnUiThread {
            val satirlar = taramaSonucu.split("\n")
            val goruntulenecekSatirSayisi = if (satirSayisi > 0) minOf(satirSayisi, satirlar.size) else satirlar.size
            taramaSonucuTextView.text = satirlar.subList(0, goruntulenecekSatirSayisi).joinToString("\n")
            val mesaj = if (textTarama) "Tarama devam ediyor..." else "Tarama tamamlandı"
            Toast.makeText(this, mesaj, Toast.LENGTH_SHORT).show()
        }
    }

    private fun taramaSonucunuIsle(taramaSonucu: String): String {
        return taramaSonucu
    }

    private fun izinKontroluVeGaleriAc() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                getContent.launch("image/*")
            }
            else -> {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {

                    Toast.makeText(
                        this,
                        "Galeriye erişim izni verilmedi. Lütfen izin veriniz.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    GALERI_IZIN_TALEBI
                )
            }
        }
    }

    private fun izinKontroluVeKameraAc() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                kameraCekimIntentiniBaslat()
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    KAMERA_IZIN_TALEBI
                )
            }
        }
    }

    private fun kameraCekimIntentiniBaslat() {
        Log.d("Kamera", "kameraCekimIntentiniBaslat çağrıldı")
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { kameraCekimIntent ->
            kameraCekimIntent.resolveActivity(packageManager)?.also {
                try {
                    imageDosyasi = resimDosyasiOlustur()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
                imageDosyasi?.also {
                    val photoURI = FileProvider.getUriForFile(this, "$packageName.provider", it)
                    kameraCekimIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    resimCek.launch(photoURI)
                }
            }
        }
    }

    private fun siyahBeyazDonustur(bitmap: Bitmap): Bitmap {
        val genislik = bitmap.width
        val yukseklik = bitmap.height

        val siyahBeyazBitmap = Bitmap.createBitmap(genislik, yukseklik, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(siyahBeyazBitmap)
        val paint = Paint()


        val renkMatrisi = ColorMatrix().apply {
            setSaturation(0f)
        }

        val tonMatrisi = floatArrayOf(
            1f, 0f, 0f, 0f, (-Color.red(Color.BLACK)).toFloat(),
            0f, 1f, 0f, 0f, (-Color.green(Color.BLACK)).toFloat(),
            0f, 0f, 1f, 0f, (-Color.blue(Color.BLACK)).toFloat(),
            0f, 0f, 0f, 1f, 0f
        )
        renkMatrisi.postConcat(ColorMatrix(tonMatrisi))
        paint.colorFilter = ColorMatrixColorFilter(renkMatrisi)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return siyahBeyazBitmap
    }

    private fun resimDosyasiOlustur(): File {
        val zamanDamgasi: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val depolamaDizini: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

        return File.createTempFile(
            "JPEG_${zamanDamgasi}_",
            ".jpg",
            depolamaDizini
        ).apply {
            guncelResimYolu = absolutePath
        }
    }

    private fun taninanMetniAl() {
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val image = InputImage.fromFilePath(this, imageDosyasi!!.toUri())

        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                taramaSonucu = ""

                for (block in result.textBlocks) {
                    if (block.lines.isNotEmpty()) {
                        for (line in block.lines) {
                            val filtrelenmisMetin = metniFiltrele(line.text)
                            if (filtrelenmisMetin.isNotEmpty()) {
                                Log.d("TextRecognition", "Filtrelenen satır: $filtrelenmisMetin")
                                taramaSonucu += "$filtrelenmisMetin\n"
                            }
                        }
                    }
                }

                taramaSonucu = taramaSonucunuIsle(taramaSonucu)
                arayuzGuncelle()
            }
            .addOnFailureListener { e ->
                taramaSonucu = "Tarama hatası oluştu"
                arayuzGuncelle()
            }
            .addOnCompleteListener {
                textTarama = false
                textRecognizer.close()
            }
    }

    private fun metniFiltrele(rawMetin: String): String {
        val izinVerilenKarakterler =
            setOf('A', 'B', 'C', 'D', 'E', 'V', 'P', 'L', 'R', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.')
        val filtrelenmisMetin = StringBuilder()

        for (karakter in rawMetin) {
            if ((karakter.isUpperCase() || karakter.isDigit() || karakter == '.') && karakter.toUpperCase() in izinVerilenKarakterler) {
                filtrelenmisMetin.append(karakter)
            }
        }

        return filtrelenmisMetin.toString()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GALERI_IZIN_TALEBI -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContent.launch("image/*")
                } else {
                    Toast.makeText(
                        this,
                        "İzin reddedildi. Galeriye erişilemiyor.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            KAMERA_IZIN_TALEBI -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    kameraCekimIntentiniBaslat()
                } else {
                    Toast.makeText(
                        this,
                        "İzin reddedildi. Kamera erişilemiyor.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    companion object {
        private var guncelResimYolu: String = ""
        private const val GALERI_IZIN_TALEBI = 1
        private const val KAMERA_IZIN_TALEBI = 2
    }
}