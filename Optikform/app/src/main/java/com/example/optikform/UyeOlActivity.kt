package com.example.optikform
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UyeOlActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uye_ol)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val editTextEmail: EditText = findViewById(R.id.editTextEmail)
        val editTextSifre: EditText = findViewById(R.id.editTextSifre)

        val buttonUyeOl: Button = findViewById(R.id.buttonUyeOl)

        buttonUyeOl.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val sifre = editTextSifre.text.toString().trim()


            if (email.isNotEmpty() && sifre.isNotEmpty() ) {

                kullaniciyiKaydet( email, sifre)
            } else {
                Toast.makeText(
                    this,
                    "E-posta ve şifre boş olamaz.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }



    private fun kullaniciyiKaydet(email: String, sifre: String) {
        auth.createUserWithEmailAndPassword(email, sifre)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Üye kaydı başarılı. Lütfen e-posta adresinizi doğrulayın.",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this, GirisYapActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "E-posta doğrulama gönderilemedi: ${verificationTask.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Üye kaydı başarısız. Hata: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
