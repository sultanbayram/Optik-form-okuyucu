package com.example.optikform

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SifremiUnuttumActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var buttonGonder: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sifremi_unuttum)

        editTextEmail = findViewById(R.id.editTextEmail)
        buttonGonder = findViewById(R.id.button_gonder)

        auth = FirebaseAuth.getInstance()

        buttonGonder.setOnClickListener {
            sifreyiSifirla()
        }
    }

    private fun sifreyiSifirla() {
        val email = editTextEmail.text.toString().trim()

        if (email.isEmpty()) {
            editTextEmail.error = "E-posta alanı boş olamaz"
            return
        }
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    Toast.makeText(this, "Şifre sıfırlama linki e-posta adresinize gönderildi.", Toast.LENGTH_SHORT).show()

                } else {
                    // İstek gönderilemedi
                    Toast.makeText(this, "Şifre sıfırlama isteği gönderilemedi." , Toast.LENGTH_SHORT).show()
                }
            }
    }
}