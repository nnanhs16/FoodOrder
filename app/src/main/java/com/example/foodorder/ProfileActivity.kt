package com.example.foodorder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorder.database.DatabaseHelper

class ProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var session: SessionManager

    private var currentUserId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)
        currentUserId = session.getUserId()

        if (!session.isLoggedIn()) {
            goToLogin()
            return
        }

        setContentView(R.layout.activity_profile)

        dbHelper = DatabaseHelper(this)

        val edtName = findViewById<EditText>(R.id.edtProfileName)
        val edtEmail = findViewById<EditText>(R.id.edtProfileEmail)
        val edtPhone = findViewById<EditText>(R.id.edtProfilePhone)
        val edtAddress = findViewById<EditText>(R.id.edtProfileAddress)

        val txtUserId = findViewById<TextView>(R.id.txtUserId)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSaveProfile)
        val btnHistory = findViewById<Button>(R.id.btnOrderHistory)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        loadProfile(
            edtName,
            edtEmail,
            edtPhone,
            edtAddress,
            txtUserId
        )

        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {

            val name = edtName.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val address = edtAddress.text.toString().trim()

            if (name.isEmpty()) {
                showMessage("Name cannot be empty")
                return@setOnClickListener
            }

            val success = dbHelper.updateUser(
                currentUserId,
                name,
                phone,
                address
            )

            if (success) {
                showMessage("Profile updated successfully")
            } else {
                showMessage("Unable to update profile")
            }
        }

        btnHistory.setOnClickListener {
            startActivity(
                Intent(this, OrderHistoryActivity::class.java)
            )
        }

        btnLogout.setOnClickListener {

            session.logout()

            val intent = Intent(this, LoginActivity::class.java)

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }
    }

    private fun loadProfile(
        edtName: EditText,
        edtEmail: EditText,
        edtPhone: EditText,
        edtAddress: EditText,
        txtUserId: TextView
    ) {

        val cursor = dbHelper.getUserById(currentUserId)

        cursor.use {

            if (it.moveToFirst()) {
                edtName.setText(it.getString(1))
                edtEmail.setText(it.getString(2))
                edtPhone.setText(it.getString(3) ?: "")
                edtAddress.setText(it.getString(4) ?: "")

                txtUserId.text = "Account ID: $currentUserId"
            } else {
                showMessage("Account not found")
                goToLogin()
            }
        }
    }

    private fun goToLogin() {

        session.logout()

        val intent = Intent(this, LoginActivity::class.java)

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
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
