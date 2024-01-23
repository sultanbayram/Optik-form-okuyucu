package com.example.optikform
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class GirisYapActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_giris_yap)

        auth = FirebaseAuth.getInstance()

        val editTextEmail: EditText = findViewById(R.id.editTextEmail)
        val editTextSifre: EditText = findViewById(R.id.editTextSifre)
        val buttonGirisYap: Button = findViewById(R.id.buttonGirisYap)
        val buttonSifremiUnuttum: Button = findViewById(R.id.buttonSifremiUnuttum)

        buttonGirisYap.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val sifre = editTextSifre.text.toString().trim()

            if (email.isNotEmpty() && sifre.isNotEmpty()) {
                kullaniciGirisi(email, sifre)
            } else {
                Toast.makeText(
                    this,
                    "E-posta ve şifre alanları boş bırakılamaz.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        buttonSifremiUnuttum.setOnClickListener {
            val intent = Intent(this, SifremiUnuttumActivity::class.java)
            startActivity(intent)
        }
    }

    private fun kullaniciGirisi(email: String, sifre: String) {
        auth.signInWithEmailAndPassword(email, sifre)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {

                        val intent = Intent(this, UyeAnaSayfaActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {

                        Toast.makeText(
                            this,
                            "E-posta adresinizi doğrulayın.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Giriş başarısız. Hata: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}