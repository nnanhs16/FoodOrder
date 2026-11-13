package com.example.foodorder

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorder.database.DatabaseHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager

    private val userId: Int
        get() = session.getUserId()

    private val money =
        NumberFormat.getNumberInstance(Locale("vi", "VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_order_detail)

        session = SessionManager(this)
        db = DatabaseHelper(this)

        // btnBack trong XML là TextView
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        loadOrder()
    }

    private fun loadOrder() {

        val orderId = intent.getIntExtra("ORDER_ID", -1)

        if (
            orderId <= 0 ||
            !session.isLoggedIn() ||
            userId <= 0
        ) {
            Toast.makeText(
                this,
                "Không tìm thấy đơn hàng",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        var orderFound = false

        try {

            db.getOrderById(orderId, userId).use { cursor ->

                if (cursor.moveToFirst()) {

                    orderFound = true

                    findViewById<TextView>(
                        R.id.txtDetailOrderId
                    ).text = "Đơn hàng #${cursor.getInt(0)}"

                    findViewById<TextView>(
                        R.id.txtDetailDate
                    ).text = formatOrderDate(
                        cursor.getString(1) ?: ""
                    )

                    findViewById<TextView>(
                        R.id.txtDetailTotal
                    ).text = "${money.format(cursor.getDouble(2))} đ"

                    findViewById<TextView>(
                        R.id.txtDetailStatus
                    ).text = cursor.getString(3) ?: "Đang xử lý"

                    findViewById<TextView>(
                        R.id.txtDetailName
                    ).text = "Người nhận: ${cursor.getString(4) ?: ""}"

                    findViewById<TextView>(
                        R.id.txtDetailPhone
                    ).text = "Số điện thoại: ${cursor.getString(5) ?: ""}"

                    findViewById<TextView>(
                        R.id.txtDetailAddress
                    ).text = "Địa chỉ: ${cursor.getString(6) ?: ""}"

                    findViewById<TextView>(
                        R.id.txtDetailPayment
                    ).text = "Thanh toán: ${cursor.getString(7) ?: ""}"
                }
            }

            if (!orderFound) {

                Toast.makeText(
                    this,
                    "Không tìm thấy đơn hàng",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
                return
            }

            loadOrderItems(orderId)

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Có lỗi khi tải chi tiết đơn hàng: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    private fun loadOrderItems(orderId: Int) {

        val container = findViewById<LinearLayout>(
            R.id.orderDetailItemsContainer
        )

        container.removeAllViews()

        var hasItems = false

        db.getOrderDetails(orderId).use { cursor ->

            while (cursor.moveToNext()) {

                hasItems = true

                val foodName = cursor.getString(1) ?: "Món ăn"
                val quantity = cursor.getInt(2)
                val price = cursor.getDouble(3)
                val subtotal = cursor.getDouble(4)

                val itemView = TextView(this).apply {

                    text = "$foodName  x$quantity\n" +
                            "Đơn giá: ${money.format(price)} đ\n" +
                            "Thành tiền: ${money.format(subtotal)} đ"

                    textSize = 15f

                    setTextColor(
                        android.graphics.Color.DKGRAY
                    )

                    setPadding(
                        0,
                        dp(10),
                        0,
                        dp(10)
                    )
                }

                container.addView(itemView)

                val divider = View(this).apply {
                    setBackgroundColor(0xFFEAEAEA.toInt())
                }

                container.addView(
                    divider,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                    )
                )
            }
        }

        if (!hasItems) {

            container.addView(
                TextView(this).apply {
                    text = "Không có thông tin món ăn trong đơn hàng."
                    textSize = 14f

                    setPadding(
                        0,
                        dp(10),
                        0,
                        dp(10)
                    )
                }
            )
        }
    }

    private fun formatOrderDate(value: String): String {

        val timestamp = value.toLongOrNull() ?: return value

        return try {

            SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale("vi", "VN")
            ).format(Date(timestamp))

        } catch (e: Exception) {
            value
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {

        if (::db.isInitialized) {
            db.close()
        }

        super.onDestroy()
    }
}
