package com.example.foodorder

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorder.database.DatabaseHelper
import java.text.NumberFormat
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var container: LinearLayout
    private lateinit var session: SessionManager

    private val userId: Int
        get() = session.getUserId()

    private val money =
        NumberFormat.getNumberInstance(Locale("vi", "VN"))

    private val orange = Color.rgb(240, 120, 54)
    private val dark = Color.rgb(41, 41, 41)
    private val gray = Color.rgb(130, 130, 130)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        db = DatabaseHelper(this)
        session = SessionManager(this)

        container = findViewById(R.id.cartItemsContainer)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnCheckout).setOnClickListener {

            if (!session.isLoggedIn()) {
                Toast.makeText(
                    this,
                    "Vui lòng đăng nhập lại",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (db.getCartTotal(userId) <= 0.0) {
                Toast.makeText(
                    this,
                    "Giỏ hàng đang trống",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startActivity(
                    Intent(this, CheckoutActivity::class.java)
                )
            }
        }

        loadCart()
    }

    private fun getFoodImage(image: String, name: String): Int {

        val key = image.lowercase()
            .substringAfterLast("/")
            .substringBeforeLast(".")

        val imageName = when {

            key.contains("burger") || key.contains("banh_mi") ||
                    name.contains("burger", true) -> "burger_bo"

            key.contains("pizza") || name.contains("pizza", true) ->
                "pizza_hai_san"

            key.contains("chicken") || key.contains("ga_ran") ||
                    name.contains("gà rán", true) -> "ga_ran"

            key.contains("noodle") || key.contains("mi_cay") ||
                    name.contains("mì cay", true) -> "mi_cay"

            key.contains("nem") || name.contains("nem chua", true) ->
                "nem_chua_ran"

            key.contains("tok") || name.contains("tokbokki", true) ->
                "tokbokki_lac_pho_mai"

            else -> ""
        }

        if (imageName.isBlank()) return 0

        return resources.getIdentifier(
            imageName,
            "drawable",
            packageName
        )
    }

    private fun loadCart() {

        container.removeAllViews()

        if (userId <= 0) {
            showEmptyCart("Vui lòng đăng nhập để xem giỏ hàng")
            updateTotal(0.0)
            return
        }

        var subtotal = 0.0

        db.getCartItems(userId).use { cursor ->

            if (cursor.count == 0) {
                showEmptyCart("Giỏ hàng của bạn đang trống!")
            }

            while (cursor.moveToNext()) {

                val foodId = cursor.getInt(1)
                val name = cursor.getString(2) ?: ""
                val price = cursor.getDouble(3)
                val quantity = cursor.getInt(4)
                val image = cursor.getString(5) ?: ""

                val itemTotal = price * quantity
                subtotal += itemTotal

                createCartItem(
                    foodId,
                    name,
                    price,
                    quantity,
                    itemTotal,
                    image
                )
            }
        }

        updateTotal(subtotal)
    }

    private fun createCartItem(
        foodId: Int,
        name: String,
        price: Double,
        quantity: Int,
        itemTotal: Double,
        image: String
    ) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(Color.WHITE, 18)
            elevation = dp(3).toFloat()
        }

        // Ảnh món ăn
        val foodImage = ImageView(this).apply {

            layoutParams = LinearLayout.LayoutParams(
                dp(105),
                dp(105)
            )

            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.rgb(255, 248, 241), 14)
            clipToOutline = true

            val imageId = getFoodImage(image, name)

            if (imageId != 0) {
                setImageResource(imageId)
            } else {
                setImageResource(
                    android.R.drawable.ic_menu_report_image
                )
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
        }

        card.addView(foodImage)

        // Thông tin món
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(12)
            }
        }

        info.addView(TextView(this).apply {
            text = name
            textSize = 16f
            setTextColor(dark)
            setTypeface(null, Typeface.BOLD)
            maxLines = 2
        })

        info.addView(TextView(this).apply {
            text = "${money.format(price)} đ / món"
            textSize = 12f
            setTextColor(gray)
            setPadding(0, dp(4), 0, dp(4))
        })

        info.addView(TextView(this).apply {
            text = "Thành tiền: ${money.format(itemTotal)} đ"
            textSize = 13f
            setTextColor(orange)
            setTypeface(null, Typeface.BOLD)
        })

        // Điều chỉnh số lượng
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, 0)
        }

        val decrease = makeActionButton("−", orange)

        val quantityText = TextView(this).apply {
            text = quantity.toString()
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(dark)
            layoutParams = LinearLayout.LayoutParams(
                dp(35),
                dp(34)
            )
        }

        val increase = makeActionButton("+", orange)

        decrease.setOnClickListener {
            db.updateCartQuantity(userId, foodId, quantity - 1)
            loadCart()
        }

        increase.setOnClickListener {
            db.updateCartQuantity(userId, foodId, quantity + 1)
            loadCart()
        }

        val delete = TextView(this).apply {
            text = "Xóa"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(
                Color.rgb(220, 80, 70),
                8
            )
            layoutParams = LinearLayout.LayoutParams(
                dp(48),
                dp(34)
            ).apply {
                marginStart = dp(8)
            }

            setOnClickListener {
                db.removeFromCart(userId, foodId)
                loadCart()

                Toast.makeText(
                    this@CartActivity,
                    "Đã xóa $name khỏi giỏ hàng",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        actions.addView(decrease)
        actions.addView(quantityText)
        actions.addView(increase)
        actions.addView(delete)

        info.addView(actions)
        card.addView(info)

        container.addView(
            card,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(14)
            }
        )
    }

    private fun makeActionButton(
        textValue: String,
        color: Int
    ): TextView {

        return TextView(this).apply {
            text = textValue
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(color, 8)
            layoutParams = LinearLayout.LayoutParams(
                dp(34),
                dp(34)
            )
        }
    }

    private fun showEmptyCart(message: String) {

        val empty = TextView(this).apply {
            text = "🛒\n\n$message"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(gray)
            setPadding(dp(20), dp(45), dp(20), dp(45))
        }

        container.addView(empty)
    }

    private fun updateTotal(total: Double) {

        findViewById<TextView>(R.id.txtSubtotal).text =
            "${money.format(total)} đ"

        findViewById<TextView>(R.id.txtTotal).text =
            "${money.format(total)} đ"
    }

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()

        if (::db.isInitialized && ::container.isInitialized) {
            loadCart()
        }
    }

    override fun onDestroy() {
        if (::db.isInitialized) db.close()
        super.onDestroy()
    }
}
