package com.example.optikform
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        supportActionBar?.show()

        supportActionBar?.apply {
            title = "OPTİK OKUYUCU"
            setDisplayShowHomeEnabled(true)
            setLogo(R.drawable.optik)
            setDisplayUseLogoEnabled(true)
        }

        val logoImageView: ImageView = findViewById(R.id.logoImageView)

        val button1: Button = findViewById(R.id.button1)
        val button2: Button = findViewById(R.id.button2)
        val button3: Button = findViewById(R.id.button3)
        button1.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {



                val intent = Intent(this@MainActivity, UyeOlActivity::class.java)
                startActivity(intent)
            }
        })

        button2.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {

                val intent = Intent(this@MainActivity, GirisYapActivity::class.java)
                startActivity(intent)
            }
        })
        button3.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {

                val intent = Intent(this@MainActivity, UyeliksizAnaSayfaActivity::class.java)
                startActivity(intent)
            }
        })
    }
}
