package com.example.foodorder

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorder.database.DatabaseHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var container: LinearLayout
    private lateinit var txtOrderCount: TextView
    private lateinit var session: SessionManager

    private val money = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    private val userId: Int
        get() = session.getUserId()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_history)

        session = SessionManager(this)
        db = DatabaseHelper(this)

        container = findViewById(R.id.orderHistoryContainer)
        txtOrderCount = findViewById(R.id.txtOrderCount)

        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        loadOrders()
    }

    private fun loadOrders() {
        container.removeAllViews()

        // Lấy tài khoản đang đăng nhập từ SessionManager
        if (!session.isLoggedIn() || userId <= 0) {
            txtOrderCount.text = "Tài khoản chưa đăng nhập"

            showEmptyState(
                "Bạn chưa đăng nhập",
                "Đăng nhập để xem lại các đơn hàng đã đặt."
            )
            return
        }

        var count = 0

        try {
            db.getOrdersByUser(userId).use { cursor ->

                while (cursor.moveToNext()) {
                    count++

                    val orderId = cursor.getInt(0)
                    val rawDate = cursor.getString(1) ?: ""
                    val total = cursor.getDouble(2)
                    val status = cursor.getString(3) ?: "Đang xử lý"

                    container.addView(
                        createOrderCard(
                            orderId,
                            rawDate,
                            total,
                            status
                        )
                    )
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()

            txtOrderCount.text = "Không thể tải đơn hàng"

            showEmptyState(
                "Đã xảy ra lỗi",
                "Không thể tải lịch sử đơn hàng. Vui lòng thử lại."
            )

            Toast.makeText(
                this,
                "Lỗi tải lịch sử đơn hàng",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        txtOrderCount.text = "$count đơn hàng"

        if (count == 0) {
            showEmptyState(
                "Chưa có đơn hàng",
                "Những đơn hàng bạn đặt sẽ được hiển thị tại đây."
            )
        }
    }

    private fun createOrderCard(
        orderId: Int,
        rawDate: String,
        total: Double,
        status: String
    ): LinearLayout {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(18),
                dp(17),
                dp(18),
                dp(16)
            )

            background = rounded(Color.WHITE, 18)
            elevation = dp(3).toFloat()
        }

        // Hàng tiêu đề và trạng thái
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "Đơn hàng #$orderId"
            textSize = 17f
            setTextColor(Color.rgb(25, 25, 25))
            setTypeface(null, Typeface.BOLD)
        }

        top.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val statusView = TextView(this).apply {
            text = status
            textSize = 11f
            setTypeface(null, Typeface.BOLD)

            setTextColor(statusColor(status))

            setPadding(
                dp(10),
                dp(7),
                dp(10),
                dp(7)
            )

            background = rounded(
                statusBackground(status),
                30
            )
        }

        top.addView(statusView)
        card.addView(top)

        // Ngày đặt hàng
        val date = TextView(this).apply {
            text = "Ngày đặt  ·  ${formatOrderDate(rawDate)}"
            textSize = 13f
            setTextColor(Color.rgb(130, 130, 130))

            setPadding(
                0,
                dp(12),
                0,
                dp(13)
            )
        }

        card.addView(date)

        // Đường phân cách
        val divider = View(this).apply {
            setBackgroundColor(Color.rgb(238, 238, 238))
        }

        card.addView(
            divider,
            LinearLayout.LayoutParams(
                -1,
                dp(1)
            )
        )

        // Hàng tổng tiền và nút chi tiết
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(13), 0, 0)
        }

        val priceColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        priceColumn.addView(
            TextView(this).apply {
                text = "TỔNG THANH TOÁN"
                textSize = 10f
                setTextColor(Color.rgb(145, 145, 145))
                setTypeface(null, Typeface.BOLD)
            }
        )

        priceColumn.addView(
            TextView(this).apply {
                text = "${money.format(total)} đ"
                textSize = 18f
                setTextColor(Color.rgb(20, 20, 20))
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(4), 0, 0)
            }
        )

        bottom.addView(
            priceColumn,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        // Nút xem chi tiết
        val detailButton = Button(this).apply {
            text = "Chi tiết  ›"
            isAllCaps = false
            textSize = 13f

            setTextColor(Color.WHITE)

            background = rounded(
                Color.rgb(25, 25, 25),
                10
            )

            setPadding(
                dp(14),
                0,
                dp(14),
                0
            )

            minHeight = dp(42)
            stateListAnimator = null

            setOnClickListener {
                val intent = Intent(
                    this@OrderHistoryActivity,
                    OrderDetailActivity::class.java
                )

                intent.putExtra("ORDER_ID", orderId)

                startActivity(intent)
            }
        }

        bottom.addView(
            detailButton,
            LinearLayout.LayoutParams(
                -2,
                dp(42)
            )
        )

        card.addView(bottom)

        return card.apply {
            layoutParams = LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {
                bottomMargin = dp(15)
            }
        }
    }

    // Giao diện khi chưa có đơn hàng
    private fun showEmptyState(
        title: String,
        message: String
    ) {

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            setPadding(
                dp(24),
                dp(42),
                dp(24),
                dp(42)
            )

            background = rounded(Color.WHITE, 18)
        }

        box.addView(
            TextView(this).apply {
                text = "▤"
                textSize = 42f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(175, 175, 175))
            }
        )

        box.addView(
            TextView(this).apply {
                text = title
                textSize = 17f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.rgb(35, 35, 35))
                gravity = Gravity.CENTER

                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(6)
                )
            }
        )

        box.addView(
            TextView(this).apply {
                text = message
                textSize = 13f
                setTextColor(Color.rgb(135, 135, 135))
                gravity = Gravity.CENTER
            }
        )

        container.addView(
            box,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )
    }

    // Màu chữ trạng thái
    private fun statusColor(status: String): Int = when {
        status.contains("hủy", true) ->
            Color.rgb(190, 55, 55)

        status.contains("giao", true) ||
                status.contains("thành công", true) ||
                status.contains("hoàn tất", true) ->
            Color.rgb(35, 130, 80)

        else ->
            Color.rgb(180, 115, 25)
    }

    // Màu nền trạng thái
    private fun statusBackground(status: String): Int = when {
        status.contains("hủy", true) ->
            Color.rgb(255, 235, 235)

        status.contains("giao", true) ||
                status.contains("thành công", true) ||
                status.contains("hoàn tất", true) ->
            Color.rgb(230, 247, 237)

        else ->
            Color.rgb(255, 245, 225)
    }

    // Tạo background bo góc
    private fun rounded(
        color: Int,
        radius: Int
    ) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    // Chuyển timestamp thành ngày giờ
    private fun formatOrderDate(value: String): String {
        val timestamp = value.toLongOrNull() ?: return value

        return SimpleDateFormat(
            "dd/MM/yyyy · HH:mm",
            Locale("vi", "VN")
        ).format(Date(timestamp))
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()

        if (::container.isInitialized) {
            loadOrders()
        }
    }

    override fun onDestroy() {
        if (::db.isInitialized) {
            db.close()
        }

        super.onDestroy()
    }
}
