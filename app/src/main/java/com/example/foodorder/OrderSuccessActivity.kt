package com.example.foodorder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorder.database.DatabaseHelper
import java.text.NumberFormat
import java.util.Locale

class OrderSuccessActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    private val userId: Int
        get() = getSharedPreferences(
            "FoodOrderSession",
            MODE_PRIVATE
        ).getInt("user_id", 0)

    private val money =
        NumberFormat.getNumberInstance(Locale("vi", "VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_success)

        db = DatabaseHelper(this)

        val orderId = intent.getIntExtra("ORDER_ID", -1)

        if (orderId <= 0) {
            finish()
            return
        }

        var total = intent.getDoubleExtra("ORDER_TOTAL", -1.0)
        var status = "Đang xử lý"

        // Lấy lại dữ liệu trực tiếp từ database để tránh hiển thị sai
        db.getOrderById(orderId, userId).use { cursor ->

            if (cursor.moveToFirst()) {
                total = cursor.getDouble(2)
                status = cursor.getString(3) ?: "Đang xử lý"
            }
        }

        findViewById<TextView>(R.id.txtSuccessOrderId).text =
            "Đơn hàng #${orderId.toString().padStart(5, '0')}"

        findViewById<TextView>(R.id.txtSuccessTotal).text =
            "${money.format(if (total >= 0.0) total else 0.0)} đ"

        findViewById<TextView>(R.id.txtSuccessStatus).text =
            "Trạng thái: $status\nThanh toán: Khi nhận hàng (COD)"

        findViewById<Button>(R.id.btnViewOrder).setOnClickListener {
            val intent = Intent(this, OrderDetailActivity::class.java)
            intent.putExtra("ORDER_ID", orderId)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnBackHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK

            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        if (::db.isInitialized) {
            db.close()
        }
        super.onDestroy()
    }
}