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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class UyeliksizActivity : AppCompatActivity() {

    private var textScanning = false
    private var imageFile: File? = null
    private var scannedText = ""
    private var kontrolText = ""
    private lateinit var spinnerSoruSayisi: Spinner
    private lateinit var scannedTextResult: TextView
    private var satirSayisi = 0
    private lateinit var textViewKontrol: TextView
    private lateinit var karsilastirButton: Button
    private var selectedSoruSayisi = 0
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                processImageUri(it, R.id.imageView)
                textScanning = true
                getRecognisedText()
            }
        }

    private val getContent1 =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                processImageUri(it, R.id.imageView1)
                textScanning = true
                getRecognisedText1()
            }
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                processImageFile(imageFile!!, R.id.imageView)
                textScanning = true
                getRecognisedText()
            }
        }

    private val takePicture1 =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                processImageFile(imageFile!!, R.id.imageView1)
                textScanning = true
                getRecognisedText1()
            }
        }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uyeliksiz)

        scannedTextResult = findViewById(R.id.tarananMetinSonucu)
        textViewKontrol = findViewById(R.id.textViewKontrol)
        findViewById<Button>(R.id.btnGallery).setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            checkPermissionAndOpenCamera()
        }

        findViewById<Button>(R.id.btnGallery1).setOnClickListener {
            checkPermissionAndOpenGallery1()
        }

        findViewById<Button>(R.id.btnCamera1).setOnClickListener {
            checkPermissionAndOpenCamera1()
        }
        karsilastirButton = findViewById(R.id.buttonKarsilastir)

        karsilastirButton.setOnClickListener {
            karsilastirButtonOnClick()
        }
        spinnerSoruSayisi = findViewById(R.id.spinnerSoruSayisi)


        val soruSayisiList = mutableListOf<String>("Soru Sayısı")
        for (i in 1..100) {
            soruSayisiList.add(i.toString())
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, soruSayisiList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSoruSayisi.adapter = adapter


        spinnerSoruSayisi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {

                val selectedValue = parentView.getItemAtPosition(position).toString()
                if (selectedValue != "Soru Sayısı") {
                    selectedSoruSayisi = selectedValue.toInt()+1
                    updateUI()
                    updateUI1()
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {

            }
        }
    }

    private fun karsilastirButtonOnClick() {

        if (selectedSoruSayisi == 0) {
            Toast.makeText(this, "Lütfen bir soru sayısı seçin.", Toast.LENGTH_SHORT).show()
            return
        }

        val scannedTextFromFirebase = textViewKontrol.text.toString()
        val scannedTextFromImage = scannedTextResult.text.toString()

        val firebaseLines = scannedTextFromFirebase.split("\n")
        val imageLines = scannedTextFromImage.split("\n")

        // Find the index of the line starting with "01."
        val startIndex = maxOf(
            firebaseLines.indexOfFirst { it.startsWith("01.") },
            imageLines.indexOfFirst { it.startsWith("01.") }
        )

        val totalLines = min(firebaseLines.size - startIndex, imageLines.size - startIndex)
        var matchingLines = 0

        for (i in 0 until min(selectedSoruSayisi, totalLines)) {
            val firebaseLine = firebaseLines[startIndex + i]
            val imageLine = imageLines[startIndex + i]

            if (firebaseLine == imageLine) {
                matchingLines++
            }
        }

        val accuracy = (matchingLines.toDouble() / min(selectedSoruSayisi, totalLines)) * 100


        findViewById<TextView>(R.id.benzerlikPuaniTextView).text = "Puan: $accuracy%"
    }

    private fun calculateSimilarity(text1: String, text2: String): Double {
        val distance = levenshteinDistance(text1, text2)
        val maxLength = maxOf(text1.length, text2.length)

        return (1 - distance.toDouble() / maxLength) * 100
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) {
            for (j in 0..n) {
                if (i == 0) {
                    dp[i][j] = j
                } else if (j == 0) {
                    dp[i][j] = i
                } else {
                    dp[i][j] = min(
                        min(dp[i - 1][j], dp[i][j - 1]),
                        dp[i - 1][j - 1] + if (s1[i - 1] == s2[j - 1]) 0 else 1
                    )
                }
            }
        }

        return dp[m][n]
    }

    private fun updateUI() {
        runOnUiThread {
            val lines = scannedText.split("\n")
            val displayLines = if (selectedSoruSayisi > 0) minOf(selectedSoruSayisi, lines.size) else lines.size
            scannedTextResult.text = lines.subList(0, displayLines).joinToString("\n")
            val message = if (textScanning) "Scanning in progress..." else "Scanning complete"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI1() {
        runOnUiThread {
            val lines = kontrolText.split("\n")
            val displayLines = if (selectedSoruSayisi > 0) minOf(selectedSoruSayisi, lines.size) else lines.size
            textViewKontrol.text = lines.subList(0, displayLines).joinToString("\n")
            val message = if (textScanning) "Scanning in progress..." else "Scanning complete"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun processScannedText(scannedText: String): String {
        return scannedText
    }

    private fun processScannedText1(scannedText: String): String {
        return kontrolText
    }

    private fun checkPermissionAndOpenGallery() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                getContent.launch("image/*")
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_GALLERY_PERMISSION
                )
            }
        }
    }

    private fun checkPermissionAndOpenGallery1() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                getContent1.launch("image/*")
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_GALLERY_PERMISSION
                )
            }
        }
    }

    private fun checkPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                dispatchTakePictureIntent()
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
            }
        }
    }

    private fun checkPermissionAndOpenCamera1() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                dispatchTakePictureIntent1()
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Log.d("Camera", "dispatchTakePictureIntent çağrıldı")
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                try {
                    imageFile = createImageFile()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
                imageFile?.also {
                    val photoURI =
                        FileProvider.getUriForFile(this, "$packageName.provider", it)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePicture.launch(photoURI)
                }
            }
        }
    }

    private fun dispatchTakePictureIntent1() {
        Log.d("Camera", "dispatchTakePictureIntent çağrıldı")
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent1 ->
            takePictureIntent1.resolveActivity(packageManager)?.also {
                try {
                    imageFile = createImageFile()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
                imageFile?.also {
                    val photoURI =
                        FileProvider.getUriForFile(this, "$packageName.provider", it)
                    takePictureIntent1.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePicture1.launch(photoURI)
                }
            }
        }
    }

    private fun convertToBlackAndWhite(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val bwBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bwBitmap)
        val paint = Paint()


        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }


        val toneMatrix = floatArrayOf(
            1f, 0f, 0f, 0f, (-Color.red(Color.BLACK)).toFloat(),
            0f, 1f, 0f, 0f, (-Color.green(Color.BLACK)).toFloat(),
            0f, 0f, 1f, 0f, (-Color.blue(Color.BLACK)).toFloat(),
            0f, 0f, 0f, 1f, 0f
        )

        colorMatrix.postConcat(ColorMatrix(toneMatrix))

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return bwBitmap
    }

    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun getRecognisedText() {
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val image = InputImage.fromFilePath(this, imageFile!!.toUri())

        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                scannedText = ""

                for (block in result.textBlocks) {
                    if (block.lines.isNotEmpty()) {
                        for (line in block.lines) {
                            val filteredText = filterText(line.text)
                            if (filteredText.isNotEmpty()) {
                                Log.d("TextRecognition", "Filtrelenen satır: $filteredText")
                                scannedText += "$filteredText\n"
                            }
                        }
                    }
                }

                scannedText = processScannedText(scannedText)
                updateUI()
            }
            .addOnFailureListener { e ->
                scannedText = "Tarama hatası oluştu"
                updateUI()
            }
            .addOnCompleteListener {
                textScanning = false
                textRecognizer.close()
            }
    }

    private fun getRecognisedText1() {
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val image = InputImage.fromFilePath(this, imageFile!!.toUri())

        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                kontrolText = ""

                for (block in result.textBlocks) {
                    if (block.lines.isNotEmpty()) {
                        for (line in block.lines) {
                            val filteredText = filterText(line.text)
                            if (filteredText.isNotEmpty()) {
                                Log.d("TextRecognition", "Filtered line: $filteredText")
                                kontrolText += "$filteredText\n"
                            }
                        }
                    }
                }

                kontrolText = processScannedText1(kontrolText)
                updateUI1()
            }
            .addOnFailureListener { e ->
                kontrolText = "Scanning error occurred"
                updateUI1()
            }
            .addOnCompleteListener {
                textScanning = false
                textRecognizer.close()
            }
    }

    private fun processImageUri(uri: android.net.Uri, imageViewId: Int) {
        val inputStream = contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        val bwBitmap = convertToBlackAndWhite(originalBitmap)

        val tempFile = File.createTempFile("temp", null, cacheDir)
        tempFile.outputStream().use { outputStream ->
            bwBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }

        imageFile = tempFile
        findViewById<ImageView>(imageViewId).setImageBitmap(bwBitmap)
    }

    private fun processImageFile(file: File, imageViewId: Int) {
        val imageBitmap = BitmapFactory.decodeFile(file.absolutePath)
        val bwBitmap = convertToBlackAndWhite(imageBitmap)

        findViewById<ImageView>(imageViewId).setImageBitmap(bwBitmap)
    }

    private fun filterText(rawText: String): String {
        val allowedCharacters = setOf('A', 'B', 'C', 'D', 'E', 'V', 'P', 'L', 'R', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.')
        val filteredText = StringBuilder()

        for (char in rawText) {
            if ((char.isUpperCase() || char.isDigit() || char == '.') && char.toUpperCase() in allowedCharacters) {
                filteredText.append(char)
            }
        }

        return filteredText.toString()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_GALLERY_PERMISSION -> {
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
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent()
                } else {
                    Toast.makeText(
                        this,
                        "İzin reddedildi. Kameraya erişilemiyor.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    companion object {
        private var currentPhotoPath: String = ""
        private const val REQUEST_GALLERY_PERMISSION = 1
        private const val REQUEST_CAMERA_PERMISSION = 2
    }
}