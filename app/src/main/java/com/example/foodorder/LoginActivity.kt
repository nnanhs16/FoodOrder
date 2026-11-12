
package com.example.foodorder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorder.database.DatabaseHelper

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)

        // Nếu đã đăng nhập thì chuyển thẳng đến trang chủ
        if (session.isLoggedIn()) {
            openHome()
            return
        }

        setContentView(R.layout.activity_login)

        dbHelper = DatabaseHelper(this)

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnGoRegister)

        // Xử lý đăng nhập
        btnLogin.setOnClickListener {

            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString()

            if (email.isBlank() || password.isBlank()) {
                showMessage("Vui lòng nhập email và mật khẩu")
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS
                    .matcher(email).matches()
            ) {
                showMessage("Email không hợp lệ")
                return@setOnClickListener
            }

            val userId = dbHelper.loginUser(email, password)

            if (userId > 0) {

                // Lưu ID bằng SessionManager
                session.saveUser(userId)

                showMessage("Đăng nhập thành công")

                openHome()

            } else {

                showMessage("Email hoặc mật khẩu không chính xác")
            }
        }

        // Chuyển sang đăng ký
        btnRegister.setOnClickListener {

            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }

    private fun openHome() {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

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

        if (::dbHelper.isInitialized) {
            dbHelper.close()
        }

        super.onDestroy()
    }
}
