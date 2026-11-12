
package com.example.foodorder

import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorder.database.DatabaseHelper
import java.text.NumberFormat
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager

    private val userId: Int
        get() = session.getUserId()

    private val money =
        NumberFormat.getNumberInstance(Locale("vi", "VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_checkout)

        db = DatabaseHelper(this)
        session = SessionManager(this)

        // Kiểm tra đăng nhập
        if (!session.isLoggedIn()) {

            Toast.makeText(
                this,
                "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(this, LoginActivity::class.java)

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
            return
        }

        // Quay lại giỏ hàng
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Hiển thị thông tin người nhận
        loadProfile()

        // Hiển thị các món trong giỏ hàng
        loadItems()

        // Xử lý đặt hàng
        findViewById<Button>(R.id.btnPlaceOrder).setOnClickListener {
            placeOrder()
        }
    }

    // =========================
    // THÔNG TIN NGƯỜI NHẬN
    // =========================

    private fun loadProfile() {

        db.getUserById(userId).use { cursor ->

            if (cursor.moveToFirst()) {

                findViewById<EditText>(R.id.edtFullName)
                    .setText(cursor.getString(1) ?: "")

                findViewById<EditText>(R.id.edtPhone)
                    .setText(cursor.getString(3) ?: "")

                findViewById<EditText>(R.id.edtAddress)
                    .setText(cursor.getString(5) ?: "")
            }
        }
    }

    // =========================
    // HIỂN THỊ GIỎ HÀNG
    // =========================

    private fun loadItems() {

        val container =
            findViewById<LinearLayout>(R.id.checkoutItemsContainer)

        container.removeAllViews()

        var hasItems = false

        db.getCartItems(userId).use { cursor ->

            while (cursor.moveToNext()) {

                hasItems = true

                val name = cursor.getString(2) ?: ""
                val price = cursor.getDouble(3)
                val quantity = cursor.getInt(4)
                val subtotal = cursor.getDouble(6)

                val itemView = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(14, 12, 14, 12)
                    setBackgroundColor(Color.WHITE)
                }

                val nameView = TextView(this).apply {
                    text = name
                    textSize = 16f
                    setTextColor(Color.rgb(40, 40, 40))
                    setTypeface(null, Typeface.BOLD)
                }

                val priceView = TextView(this).apply {
                    text = "${money.format(price)} đ x $quantity"
                    textSize = 13f
                    setTextColor(Color.GRAY)
                    setPadding(0, 5, 0, 5)
                }

                val subtotalView = TextView(this).apply {
                    text = "Thành tiền: ${money.format(subtotal)} đ"
                    textSize = 14f
                    setTextColor(Color.rgb(240, 120, 54))
                    setTypeface(null, Typeface.BOLD)
                }

                itemView.addView(nameView)
                itemView.addView(priceView)
                itemView.addView(subtotalView)

                container.addView(
                    itemView,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(10)
                    }
                )
            }
        }

        if (!hasItems) {

            val emptyView = TextView(this).apply {
                text = "Giỏ hàng đang trống"
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(Color.GRAY)
                setPadding(10, 30, 10, 30)
            }

            container.addView(emptyView)
        }

        val total = db.getCartTotal(userId)

        findViewById<TextView>(R.id.txtCheckoutTotal).text =
            "${money.format(total)} đ"
    }

    // =========================
    // ĐẶT HÀNG - COD
    // =========================

    private fun placeOrder() {

        val name = findViewById<EditText>(R.id.edtFullName)
            .text.toString().trim()

        val phone = findViewById<EditText>(R.id.edtPhone)
            .text.toString().trim()

        val address = findViewById<EditText>(R.id.edtAddress)
            .text.toString().trim()

        // Kiểm tra thông tin giao hàng
        if (name.isBlank() || phone.isBlank() || address.isBlank()) {

            Toast.makeText(
                this,
                "Vui lòng nhập đầy đủ thông tin giao hàng",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (!phone.matches(Regex("^[0-9]{9,11}$"))) {

            Toast.makeText(
                this,
                "Số điện thoại không hợp lệ",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Kiểm tra đăng nhập
        if (!session.isLoggedIn() || userId <= 0) {

            Toast.makeText(
                this,
                "Vui lòng đăng nhập lại",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Lấy tổng tiền trước khi tạo đơn
        val total = db.getCartTotal(userId)

        if (total <= 0.0) {

            Toast.makeText(
                this,
                "Giỏ hàng đang trống",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Chỉ thanh toán khi nhận hàng
        val paymentMethod = "Thanh toán khi nhận hàng (COD)"

        // Tạo đơn hàng trong SQLite
        val orderId = db.createOrder(
            userId,
            name,
            phone,
            address,
            paymentMethod
        )

        if (orderId <= 0L) {

            Toast.makeText(
                this,
                "Đặt hàng thất bại. Vui lòng thử lại.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Toast.makeText(
            this,
            "Đặt hàng thành công!",
            Toast.LENGTH_SHORT
        ).show()

        // Chuyển sang màn hình đặt hàng thành công
        val intent = Intent(
            this,
            OrderSuccessActivity::class.java
        ).apply {

            putExtra("ORDER_ID", orderId.toInt())
            putExtra("ORDER_TOTAL", total)
        }

        startActivity(intent)
        finish()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {

        if (::db.isInitialized) {
            db.close()
        }

        super.onDestroy()
    }
}
