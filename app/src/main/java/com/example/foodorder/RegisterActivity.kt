package com.example.foodorder

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorder.database.DatabaseHelper

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = DatabaseHelper(this)

        val edtName = findViewById<EditText>(R.id.edtFullName)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPhone = findViewById<EditText>(R.id.edtPhone)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val edtConfirm = findViewById<EditText>(R.id.edtConfirmPassword)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnBack = findViewById<TextView>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {

            val name = edtName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val password = edtPassword.text.toString()
            val confirm = edtConfirm.text.toString()

            when {

                name.isEmpty() || email.isEmpty() ||
                        phone.isEmpty() || password.isEmpty() ||
                        confirm.isEmpty() -> {
                    showMessage("Please fill in all fields")
                }

                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    showMessage("Invalid email address")
                }

                password.length < 8 -> {
                    showMessage("Password must have at least 8 characters")
                }

                password != confirm -> {
                    showMessage("Passwords do not match")
                }

                else -> {

                    val userId = dbHelper.registerUser(
                        name,
                        email,
                        phone,
                        password
                    )

                    if (userId > 0) {

                        Toast.makeText(
                            this,
                            "Registration successful. Please login.",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()

                    } else {
                        showMessage("Email already exists")
                    }
                }
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::dbHelper.isInitialized) {
            dbHelper.close()
        }
    }
}
