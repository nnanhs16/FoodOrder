package com.example.foodorder.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "FoodOrder.db"
        private const val DATABASE_VERSION = 4

        const val TABLE_USERS = "Users"
        const val TABLE_FOOD = "Food"
        const val TABLE_INGREDIENT = "Ingredient"
        const val TABLE_CART = "Cart"
        const val TABLE_ORDER = "Orders"
        const val TABLE_ORDER_DETAIL = "OrderDetail"

        private const val GUEST_USER_ID = 0
    }

    // =====================================================
    // TAO DATABASE
    // =====================================================

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE Users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL COLLATE NOCASE UNIQUE,
                phone TEXT,
                password_hash TEXT NOT NULL,
                address TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE Food (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                price REAL NOT NULL,
                description TEXT,
                image TEXT,
                category TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE Ingredient (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                food_id INTEGER NOT NULL,
                ingredient_name TEXT NOT NULL,
                FOREIGN KEY(food_id) REFERENCES Food(id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE Cart (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                food_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL DEFAULT 1,
                UNIQUE(user_id, food_id),
                FOREIGN KEY(user_id) REFERENCES Users(id),
                FOREIGN KEY(food_id) REFERENCES Food(id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE Orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                order_date TEXT NOT NULL,
                total_price REAL NOT NULL,
                status TEXT NOT NULL,
                delivery_name TEXT,
                delivery_phone TEXT,
                delivery_address TEXT,
                payment_method TEXT,
                FOREIGN KEY(user_id) REFERENCES Users(id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE OrderDetail (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id INTEGER NOT NULL,
                food_id INTEGER NOT NULL,
                food_name TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                price REAL NOT NULL,
                FOREIGN KEY(order_id) REFERENCES Orders(id),
                FOREIGN KEY(food_id) REFERENCES Food(id)
            )
            """.trimIndent()
        )

        createIndexes(db)
        insertSampleData(db)
    }

    // =====================================================
    // NANG CAP DATABASE
    // =====================================================

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        if (oldVersion < 2) {
            migrateToVersion2(db)
        }

        if (oldVersion < 3) {
            addColumnIfMissing(
                db,
                "Orders",
                "delivery_name",
                "TEXT"
            )

            addColumnIfMissing(
                db,
                "Orders",
                "delivery_phone",
                "TEXT"
            )

            addColumnIfMissing(
                db,
                "Orders",
                "delivery_address",
                "TEXT"
            )

            addColumnIfMissing(
                db,
                "Orders",
                "payment_method",
                "TEXT"
            )
        }
        if (oldVersion < 4) {
            addColumnIfMissing(db, "Orders", "delivery_name", "TEXT")
            addColumnIfMissing(db, "Orders", "delivery_phone", "TEXT")
            addColumnIfMissing(db, "Orders", "delivery_address", "TEXT")
            addColumnIfMissing(db, "Orders", "payment_method", "TEXT")
        }

        createIndexes(db)
        insertSampleData(db)
    }

    private fun columnExists(
        db: SQLiteDatabase,
        table: String,
        column: String
    ): Boolean {

        db.rawQuery(
            "PRAGMA table_info($table)",
            null
        ).use { cursor ->

            while (cursor.moveToNext()) {
                if (
                    cursor.getString(1).equals(
                        column,
                        ignoreCase = true
                    )
                ) {
                    return true
                }
            }
        }

        return false
    }

    private fun addColumnIfMissing(
        db: SQLiteDatabase,
        table: String,
        column: String,
        type: String
    ) {

        if (!columnExists(db, table, column)) {
            db.execSQL(
                "ALTER TABLE $table ADD COLUMN $column $type"
            )
        }
    }

    private fun migrateToVersion2(db: SQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS Users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL COLLATE NOCASE UNIQUE,
                phone TEXT,
                password_hash TEXT NOT NULL,
                address TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )

        val guest = ContentValues().apply {
            put("id", GUEST_USER_ID)
            put("full_name", "Guest")
            put("email", "guest@local")
            put("password_hash", "")
        }

        db.insertWithOnConflict(
            TABLE_USERS,
            null,
            guest,
            SQLiteDatabase.CONFLICT_IGNORE
        )

        // Chi di chuyen gio hang neu Cart chua co user_id
        if (!columnExists(db, TABLE_CART, "user_id")) {

            db.execSQL("ALTER TABLE Cart RENAME TO Cart_old_v1")

            db.execSQL(
                """
                CREATE TABLE Cart (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    food_id INTEGER NOT NULL,
                    quantity INTEGER NOT NULL DEFAULT 1,
                    UNIQUE(user_id, food_id),
                    FOREIGN KEY(user_id) REFERENCES Users(id),
                    FOREIGN KEY(food_id) REFERENCES Food(id)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO Cart (id, user_id, food_id, quantity)
                SELECT id, 0, food_id, quantity
                FROM Cart_old_v1
                """.trimIndent()
            )

            db.execSQL("DROP TABLE Cart_old_v1")
        }

        // Them user_id vao Orders neu chua co
        if (!columnExists(db, TABLE_ORDER, "user_id")) {
            db.execSQL(
                """
                ALTER TABLE Orders
                ADD COLUMN user_id INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
        }
    }

    private fun createIndexes(db: SQLiteDatabase) {

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_cart_user ON Cart(user_id)"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_orders_user ON Orders(user_id)"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_ingredient_food ON Ingredient(food_id)"
        )
    }

    // =====================================================
    // DU LIEU MON AN MAU
    // =====================================================

    private fun insertSampleData(db: SQLiteDatabase) {

        val foods = listOf(
            arrayOf(
                "Burger Bò",
                "50000",
                "Burger bò phô mai thơm ngon",
                "burger",
                "Burger"
            ),
            arrayOf(
                "Pizza Hải Sản",
                "120000",
                "Pizza tôm mực phô mai",
                "pizza",
                "Pizza"
            ),
            arrayOf(
                "Gà Rán",
                "80000",
                "Gà rán giòn tan",
                "chicken",
                "Chicken"
            ),
            arrayOf(
                "Mì Cay",
                "45000",
                "Mì cay hải sản Hàn Quốc",
                "noodles",
                "Noodles"
            )
        )

        for (food in foods) {

            val foodId = db.rawQuery(
                "SELECT id FROM Food WHERE name = ?",
                arrayOf(food[0])
            ).use { cursor ->

                if (cursor.moveToFirst()) {
                    cursor.getLong(0)
                } else {
                    -1L
                }
            }

            if (foodId != -1L) {
                continue
            }

            val values = ContentValues().apply {
                put("name", food[0])
                put("price", food[1].toDouble())
                put("description", food[2])
                put("image", food[3])
                put("category", food[4])
            }

            val newFoodId = db.insert(
                TABLE_FOOD,
                null,
                values
            )

            if (newFoodId == -1L) {
                continue
            }

            val ingredients = when (food[0]) {

                "Burger Bò" -> listOf(
                    "Bánh mì",
                    "Thịt bò",
                    "Phô mai",
                    "Xà lách"
                )

                "Pizza Hải Sản" -> listOf(
                    "Bột mì",
                    "Tôm",
                    "Mực",
                    "Phô mai"
                )

                "Gà Rán" -> listOf(
                    "Thịt gà",
                    "Bột chiên",
                    "Gia vị"
                )

                else -> listOf(
                    "Mì",
                    "Tôm",
                    "Mực",
                    "Ớt"
                )
            }

            for (ingredient in ingredients) {

                val ingredientValues = ContentValues().apply {
                    put("food_id", newFoodId)
                    put("ingredient_name", ingredient)
                }

                db.insert(
                    TABLE_INGREDIENT,
                    null,
                    ingredientValues
                )
            }
        }
    }

    // =====================================================
    // MA HOA MAT KHAU
    // =====================================================

    private fun generateSalt(): ByteArray {

        return ByteArray(16).also {
            SecureRandom().nextBytes(it)
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {

        return bytes.joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }

    private fun hexToBytes(hex: String): ByteArray {

        return ByteArray(hex.length / 2) { index ->

            hex.substring(
                index * 2,
                index * 2 + 2
            ).toInt(16).toByte()
        }
    }

    private fun hashPassword(
        password: String,
        salt: ByteArray
    ): String {

        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            120000,
            256
        )

        return try {

            val factory = SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA1"
            )

            bytesToHex(
                factory.generateSecret(spec).encoded
            )

        } finally {
            spec.clearPassword()
        }
    }

    // =====================================================
    // TAI KHOAN
    // =====================================================

    fun registerUser(
        fullName: String,
        email: String,
        phone: String,
        password: String
    ): Long {

        if (
            fullName.isBlank() ||
            email.isBlank() ||
            password.isBlank()
        ) {
            return -1L
        }

        val salt = generateSalt()
        val passwordHash = hashPassword(password, salt)

        val values = ContentValues().apply {
            put("full_name", fullName.trim())
            put("email", email.trim())
            put("phone", phone.trim())
            put(
                "password_hash",
                bytesToHex(salt) + ":" + passwordHash
            )
        }

        return writableDatabase.insertWithOnConflict(
            TABLE_USERS,
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    fun loginUser(
        email: String,
        password: String
    ): Int {

        if (email.isBlank() || password.isBlank()) {
            return -1
        }

        val cursor = readableDatabase.rawQuery(
            """
            SELECT id, password_hash
            FROM Users
            WHERE email = ? COLLATE NOCASE
            """.trimIndent(),
            arrayOf(email.trim())
        )

        cursor.use {

            if (!it.moveToFirst()) {
                return -1
            }

            val userId = it.getInt(0)
            val storedHash = it.getString(1) ?: ""

            val parts = storedHash.split(":")

            if (parts.size != 2) {
                return -1
            }

            val salt = try {
                hexToBytes(parts[0])
            } catch (e: Exception) {
                return -1
            }

            val inputHash = hashPassword(
                password,
                salt
            )

            return if (
                inputHash.equals(parts[1], ignoreCase = true)
            ) {
                userId
            } else {
                -1
            }
        }
    }

    fun getUserById(userId: Int): Cursor {

        return readableDatabase.rawQuery(
            """
            SELECT id, full_name, email, phone, address
            FROM Users
            WHERE id = ?
            """.trimIndent(),
            arrayOf(userId.toString())
        )
    }

    fun updateUser(
        userId: Int,
        fullName: String,
        phone: String,
        address: String
    ): Boolean {

        if (userId <= 0 || fullName.isBlank()) {
            return false
        }

        val values = ContentValues().apply {
            put("full_name", fullName.trim())
            put("phone", phone.trim())
            put("address", address.trim())
        }

        return writableDatabase.update(
            TABLE_USERS,
            values,
            "id = ?",
            arrayOf(userId.toString())
        ) > 0
    }

    // =====================================================
    // MON AN
    // =====================================================

    fun getAllFoods(): Cursor {

        return readableDatabase.rawQuery(
            """
            SELECT id, name, price, description, image, category
            FROM Food
            ORDER BY id
            """.trimIndent(),
            null
        )
    }

    fun searchFoods(keyword: String): Cursor {

        return readableDatabase.rawQuery(
            """
            SELECT id, name, price, description, image, category
            FROM Food
            WHERE name LIKE ? OR category LIKE ?
            ORDER BY name
            """.trimIndent(),
            arrayOf(
                "%$keyword%",
                "%$keyword%"
            )
        )
    }

    fun getFoodsByCategory(category: String): Cursor {

        return readableDatabase.rawQuery(
            """
            SELECT id, name, price, description, image, category
            FROM Food
            WHERE category = ?
            ORDER BY name
            """.trimIndent(),
            arrayOf(category)
        )
    }

    fun getFoodById(foodId: Int): Cursor {

        return readableDatabase.rawQuery(
            """
            SELECT id, name, price, description, image, category
            FROM Food
            WHERE id = ?
            """.trimIndent(),
            arrayOf(foodId.toString())
        )
    }

    fun getIngredients(foodId: Int): Cursor {

        return readableDatabase.rawQuery(
            """
            SELECT ingredient_name
            FROM Ingredient
            WHERE food_id = ?
            """.trimIndent(),
            arrayOf(foodId.toString())
        )
    }

    // =====================================================
    // GIO HANG
    // =====================================================

    fun addToCart(
        userId: Int,
        foodId: Int,
        quantity: Int = 1
    ): Boolean {

        if (
            userId <= 0 ||
            foodId <= 0 ||
            quantity <= 0
        ) {
            return false
        }

        val db = writableDatabase

        return try {

            db.beginTransaction()

            val userExists = db.rawQuery(
                "SELECT id FROM Users WHERE id = ?",
                arrayOf(userId.toString())
            ).use {
                it.moveToFirst()
            }

            if (!userExists) {
                throw Exception("Không tìm thấy tài khoản")
            }

            val foodExists = db.rawQuery(
                "SELECT id FROM Food WHERE id = ?",
                arrayOf(foodId.toString())
            ).use {
                it.moveToFirst()
            }

            if (!foodExists) {
                throw Exception("Không tìm thấy món ăn")
            }

            val oldQuantity = db.rawQuery(
                """
                SELECT quantity
                FROM Cart
                WHERE user_id = ? AND food_id = ?
                """.trimIndent(),
                arrayOf(
                    userId.toString(),
                    foodId.toString()
                )
            ).use { cursor ->

                if (cursor.moveToFirst()) {
                    cursor.getInt(0)
                } else {
                    0
                }
            }

            val success: Boolean

            if (oldQuantity > 0) {

                val values = ContentValues().apply {
                    put(
                        "quantity",
                        oldQuantity + quantity
                    )
                }

                success = db.update(
                    TABLE_CART,
                    values,
                    "user_id = ? AND food_id = ?",
                    arrayOf(
                        userId.toString(),
                        foodId.toString()
                    )
                ) > 0

            } else {

                val values = ContentValues().apply {
                    put("user_id", userId)
                    put("food_id", foodId)
                    put("quantity", quantity)
                }

                success = db.insert(
                    TABLE_CART,
                    null,
                    values
                ) != -1L
            }

            if (!success) {
                throw Exception("Không thể thêm món vào giỏ")
            }

            db.setTransactionSuccessful()
            true

        } catch (e: Exception) {

            android.util.Log.e(
                "DatabaseHelper",
                "Lỗi thêm món vào giỏ hàng",
                e
            )

            false

        } finally {

            if (db.inTransaction()) {
                db.endTransaction()
            }
        }
    }

    fun getCartItems(userId: Int): Cursor {

        return readableDatabase.rawQuery(
            """
            SELECT
                Cart.id,
                Cart.food_id,
                Food.name,
                Food.price,
                Cart.quantity,
                Food.image,
                (Food.price * Cart.quantity) AS subtotal
            FROM Cart
            INNER JOIN Food ON Cart.food_id = Food.id
            WHERE Cart.user_id = ?
            ORDER BY Cart.id DESC
            """.trimIndent(),
            arrayOf(userId.toString())
        )
    }

    fun updateCartQuantity(
        userId: Int,
        foodId: Int,
        quantity: Int
    ): Boolean {

        if (userId <= 0 || foodId <= 0) {
            return false
        }

        if (quantity <= 0) {
            return removeFromCart(userId, foodId)
        }

        val values = ContentValues().apply {
            put("quantity", quantity)
        }

        return writableDatabase.update(
            TABLE_CART,
            values,
            "user_id = ? AND food_id = ?",
            arrayOf(
                userId.toString(),
                foodId.toString()
            )
        ) > 0
    }

    fun removeFromCart(
        userId: Int,
        foodId: Int
    ): Boolean {

        if (userId <= 0 || foodId <= 0) {
            return false
        }

        return writableDatabase.delete(
            TABLE_CART,
            "user_id = ? AND food_id = ?",
            arrayOf(
                userId.toString(),
                foodId.toString()
            )
        ) > 0
    }

    fun clearCart(userId: Int) {

        if (userId <= 0) return

        writableDatabase.delete(
            TABLE_CART,
            "user_id = ?",
            arrayOf(userId.toString())
        )
    }

    fun getCartTotal(userId: Int): Double {

        if (userId <= 0) return 0.0

        val cursor = readableDatabase.rawQuery(
            """
            SELECT SUM(Food.price * Cart.quantity)
            FROM Cart
            INNER JOIN Food ON Cart.food_id = Food.id
            WHERE Cart.user_id = ?
            """.trimIndent(),
            arrayOf(userId.toString())
        )

        cursor.use {

            return if (
                it.moveToFirst() && !it.isNull(0)
            ) {
                it.getDouble(0)
            } else {
                0.0
            }
        }
    }

    // =====================================================
    // THANH TOAN / TAO DON HANG
    // =====================================================

    fun createOrder(
        userId: Int,
        name: String,
        phone: String,
        address: String,
        paymentMethod: String
    ): Long {

        if (
            userId <= 0 ||
            name.isBlank() ||
            phone.isBlank() ||
            address.isBlank() ||
            paymentMethod.isBlank()
        ) {
            return -1L
        }

        val db = writableDatabase
        var orderId = -1L

        try {

            db.beginTransaction()

            val items = mutableListOf<OrderItem>()

            db.rawQuery(
                """
                SELECT
                    Food.id,
                    Food.name,
                    Food.price,
                    Cart.quantity
                FROM Cart
                INNER JOIN Food ON Cart.food_id = Food.id
                WHERE Cart.user_id = ?
                """.trimIndent(),
                arrayOf(userId.toString())
            ).use { cursor ->

                while (cursor.moveToNext()) {

                    items.add(
                        OrderItem(
                            foodId = cursor.getInt(0),
                            foodName = cursor.getString(1) ?: "",
                            price = cursor.getDouble(2),
                            quantity = cursor.getInt(3)
                        )
                    )
                }
            }

            if (items.isEmpty()) {
                throw Exception("Giỏ hàng đang trống")
            }

            val total = items.fold(0.0) { sum, item ->
                sum + item.price * item.quantity
            }

            val orderValues = ContentValues().apply {
                put("user_id", userId)
                put(
                    "order_date",
                    System.currentTimeMillis().toString()
                )
                put("total_price", total)
                put("status", "Đang xử lý")
                put("delivery_name", name.trim())
                put("delivery_phone", phone.trim())
                put("delivery_address", address.trim())
                put("payment_method", paymentMethod)
            }

            orderId = db.insert(
                TABLE_ORDER,
                null,
                orderValues
            )

            if (orderId == -1L) {
                throw Exception("Không thể tạo đơn hàng")
            }

            for (item in items) {

                val detailValues = ContentValues().apply {
                    put("order_id", orderId)
                    put("food_id", item.foodId)
                    put("food_name", item.foodName)
                    put("quantity", item.quantity)
                    put("price", item.price)
                }

                val detailId = db.insert(
                    TABLE_ORDER_DETAIL,
                    null,
                    detailValues
                )

                if (detailId == -1L) {
                    throw Exception(
                        "Không thể lưu chi tiết đơn hàng"
                    )
                }
            }

            val deletedRows = db.delete(
                TABLE_CART,
                "user_id = ?",
                arrayOf(userId.toString())
            )

            if (deletedRows <= 0) {
                throw Exception("Không thể cập nhật giỏ hàng")
            }

            db.setTransactionSuccessful()

        } catch (e: Exception) {

            android.util.Log.e(
                "DatabaseHelper",
                "Lỗi tạo đơn hàng",
                e
            )

            orderId = -1L

        } finally {

            if (db.inTransaction()) {
                db.endTransaction()
            }
        }

        return orderId
    }

    // =====================================================
    // LICH SU DON HANG
    // =====================================================

    fun getOrdersByUser(userId: Int): Cursor {

        return readableDatabase.rawQuery(
            """
            SELECT id, order_date, total_price, status
            FROM Orders
            WHERE user_id = ?
            ORDER BY id DESC
            """.trimIndent(),
            arrayOf(userId.toString())
        )
    }

    fun getOrderById(
        orderId: Int,
        userId: Int
    ): Cursor {

        return readableDatabase.rawQuery(
            """
            SELECT
                id,
                order_date,
                total_price,
                status,
                delivery_name,
                delivery_phone,
                delivery_address,
                payment_method
            FROM Orders
            WHERE id = ? AND user_id = ?
            """.trimIndent(),
            arrayOf(
                orderId.toString(),
                userId.toString()
            )
        )
    }

    fun getOrderDetails(orderId: Int): Cursor {

        return readableDatabase.rawQuery(
            """
            SELECT
                food_id,
                food_name,
                quantity,
                price,
                (quantity * price) AS subtotal
            FROM OrderDetail
            WHERE order_id = ?
            """.trimIndent(),
            arrayOf(orderId.toString())
        )
    }

    // =====================================================
    // DATA CLASS
    // =====================================================

    data class OrderItem(
        val foodId: Int,
        val foodName: String,
        val price: Double,
        val quantity: Int
    )
}