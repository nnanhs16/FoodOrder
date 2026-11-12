package com.example.foodorder

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorder.database.DatabaseHelper
import java.text.NumberFormat
import java.util.Locale

class FoodDetailActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    private var foodId = -1
    private var quantity = 1

    private val session by lazy { SessionManager(this) }
    private val userId: Int
        get() = session.getUserId()

    private val money =
        NumberFormat.getNumberInstance(Locale("vi", "VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!session.isLoggedIn()) {
            goToLogin()
            return
        }

        setContentView(R.layout.activity_food_detail)

        db = DatabaseHelper(this)
        foodId = intent.getIntExtra("FOOD_ID", -1)

        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.btnDecrease).setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateQuantity()
            }
        }

        findViewById<TextView>(R.id.btnIncrease).setOnClickListener {
            quantity++
            updateQuantity()
        }

        findViewById<Button>(R.id.btnAddToCart).setOnClickListener {

            if (foodId <= 0) {
                Toast.makeText(
                    this,
                    "Mã món ăn không hợp lệ",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val currentUserId = session.getUserId()

            val success = try {
                db.addToCart(currentUserId, foodId, quantity)
            } catch (e: Exception) {
                android.util.Log.e(
                    "FoodDetailActivity",
                    "Lỗi thêm món vào giỏ",
                    e
                )
                false
            }

            if (success) {
                Toast.makeText(
                    this,
                    "Đã thêm món ăn vào giỏ hàng",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            } else {
                Toast.makeText(
                    this,
                    "Không thể thêm món ăn vào giỏ hàng",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        if (foodId <= 0) {
            Toast.makeText(
                this,
                "Mã món ăn không hợp lệ",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        loadFood()
    }

    private fun updateQuantity() {
        findViewById<TextView>(R.id.txtQuantity).text =
            quantity.toString()
    }

    private fun loadFood() {

        var found = false

        db.getFoodById(foodId).use { cursor ->

            if (cursor.moveToFirst()) {
                found = true

                findViewById<TextView>(R.id.txtFoodName).text =
                    cursor.getString(1) ?: ""

                findViewById<TextView>(R.id.txtFoodPrice).text =
                    "${money.format(cursor.getDouble(2))} đ"

                findViewById<TextView>(R.id.txtFoodDescription).text =
                    cursor.getString(3) ?: ""

                val image = cursor.getString(4) ?: ""

                findViewById<TextView>(R.id.imgFoodDetail).text =
                    when (image.lowercase(Locale.ROOT)) {
                        "burger", "burger_bo" -> "🍔"
                        "pizza", "pizza_hai_san" -> "🍕"
                        "chicken", "ga_ran" -> "🍗"
                        "noodles", "mi_cay" -> "🍜"
                        else -> "🍽️"
                    }
            }
        }

        if (!found) {
            Toast.makeText(
                this,
                "Không tìm thấy món ăn",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        val ingredients = mutableListOf<String>()

        db.getIngredients(foodId).use { cursor ->
            while (cursor.moveToNext()) {
                ingredients.add(cursor.getString(0))
            }
        }

        findViewById<TextView>(R.id.txtIngredients).text =
            if (ingredients.isEmpty()) {
                "Chưa có thông tin thành phần."
            } else {
                ingredients.joinToString(", ")
            }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        if (::db.isInitialized) {
            db.close()
        }

        super.onDestroy()
    }
}