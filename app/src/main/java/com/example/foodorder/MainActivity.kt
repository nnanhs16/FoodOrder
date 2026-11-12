
package com.example.foodorder

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.foodorder.database.DatabaseHelper
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var foodContainer: LinearLayout
    private lateinit var search: EditText

    private lateinit var session: SessionManager
    private var category = ""

    private val userId: Int
        get() = session.getUserId()

    private val money =
        NumberFormat.getNumberInstance(Locale("vi", "VN"))

    private val orange = Color.rgb(240, 120, 54)
    private val dark = Color.rgb(41, 41, 41)
    private val gray = Color.rgb(130, 130, 130)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)
        session = SessionManager(this)

        foodContainer = findViewById(R.id.foodContainer)
        search = findViewById(R.id.edtSearch)

        setupNavigation()
        setupCategories()
        setupSearch()
        loadFoods()
    }

    private fun setupNavigation() {

        findViewById<View>(R.id.btnCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        findViewById<View>(R.id.btnOrders).setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        findViewById<View>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setupCategories() {

        val categories = mapOf(
            R.id.catAll to "",
            R.id.catBurger to "Burger",
            R.id.catPizza to "Pizza",
            R.id.catChicken to "Chicken",
            R.id.catNoodles to "Noodles"
        )

        categories.forEach { (viewId, value) ->
            findViewById<TextView>(viewId).setOnClickListener {
                category = value
                updateCategoryStyle()
                loadFoods()
            }
        }

        updateCategoryStyle()
    }

    private fun updateCategoryStyle() {

        val categories = listOf(
            R.id.catAll to "",
            R.id.catBurger to "Burger",
            R.id.catPizza to "Pizza",
            R.id.catChicken to "Chicken",
            R.id.catNoodles to "Noodles"
        )

        categories.forEach { (id, value) ->

            val view = findViewById<TextView>(id)

            if (category == value) {
                view.setBackgroundColor(orange)
                view.setTextColor(Color.WHITE)
                view.setTypeface(null, Typeface.BOLD)
            } else {
                view.setBackgroundColor(Color.WHITE)
                view.setTextColor(dark)
                view.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    private fun setupSearch() {

        search.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                loadFoods()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
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

    private fun loadFoods() {

        foodContainer.removeAllViews()

        val keyword = search.text.toString().trim()

        val cursor = when {
            keyword.isNotEmpty() -> db.searchFoods(keyword)
            category.isNotEmpty() -> db.getFoodsByCategory(category)
            else -> db.getAllFoods()
        }

        cursor.use {

            if (it.count == 0) {
                foodContainer.addView(TextView(this).apply {
                    text = "Không tìm thấy món ăn nào."
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setTextColor(gray)
                    setPadding(16, 45, 16, 45)
                })
                return
            }

            while (it.moveToNext()) {

                createFoodCard(
                    it.getInt(0),
                    it.getString(1) ?: "",
                    it.getDouble(2),
                    it.getString(3) ?: "",
                    it.getString(4) ?: ""
                )
            }
        }
    }

    private fun createFoodCard(
        id: Int,
        name: String,
        price: Double,
        description: String,
        image: String
    ) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(Color.WHITE, 18)
            elevation = dp(3).toFloat()
        }

        val foodImage = ImageView(this).apply {

            layoutParams = LinearLayout.LayoutParams(
                dp(105), dp(105)
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
            text = description
            textSize = 12f
            setTextColor(gray)
            maxLines = 2
            setPadding(0, dp(4), 0, dp(5))
        })

        info.addView(TextView(this).apply {
            text = "${money.format(price)} đ"
            textSize = 15f
            setTextColor(orange)
            setTypeface(null, Typeface.BOLD)
        })

        card.addView(info)

        val addButton = TextView(this).apply {
            text = "+"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            background = rounded(orange, 12)

            layoutParams = LinearLayout.LayoutParams(
                dp(40), dp(40)
            ).apply {
                marginStart = dp(8)
            }

            setOnClickListener {

                if (!session.isLoggedIn()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Vui lòng đăng nhập để mua hàng",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val currentUserId = session.getUserId()

                val added = db.addToCart(currentUserId, id)

                Toast.makeText(
                    this@MainActivity,
                    if (added) "Đã thêm $name vào giỏ hàng"
                    else "Không thể thêm món ăn",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        card.addView(addButton)

        card.setOnClickListener {
            startActivity(
                Intent(this, FoodDetailActivity::class.java)
                    .putExtra("FOOD_ID", id)
            )
        }

        foodContainer.addView(
            card,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(14)
            }
        )
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
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

        if (::db.isInitialized && ::foodContainer.isInitialized) {
            loadFoods()
        }
    }

    override fun onDestroy() {
        if (::db.isInitialized) db.close()
        super.onDestroy()
    }
}
