package com.eggbucket.eggbucket_b2c

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import android.util.TypedValue
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response // Import Retrofit2.Response


class ViewOrderActivity : AppCompatActivity() {

    // UI elements
    private lateinit var buildingNameTextView: TextView
    private lateinit var locationNameTextView: TextView
    private lateinit var itemsContainerLayout: LinearLayout
    private lateinit var backButton: ImageView
    private lateinit var subtotalTextView: TextView
    private lateinit var deliveryTextView: TextView
    private lateinit var grandTotalTextView: TextView
    private lateinit var deliveryPartnerNameTextView: TextView
    private lateinit var orderNoteTextView: TextView
    private lateinit var thankYouButton: Button

    // SharedPreferences for user data
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var phoneNumber: String

    // Map for product display names to backend API names (for price lookup)
    private val productApiNameMap = mapOf(
        "Eggs x 6" to "6pc_tray",
        "Eggs x 12" to "12pc_tray",
        "Eggs x 30" to "30pc_tray"
    )
    // Reverse map to get display name from backend name for UI
    private val apiToDisplayNameMap = mapOf(
        "6pc_tray" to "Pack of 6",
        "12pc_tray" to "Pack of 12",
        "30pc_tray" to "Pack of 30"
    )

    private var productPriceMap: Map<String, Double> = emptyMap()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_view_order)

        // Initialize UI elements
        buildingNameTextView = findViewById(R.id.buildingName)
        locationNameTextView = findViewById(R.id.locationName)
        itemsContainerLayout = findViewById(R.id.itemsContainer)
        backButton = findViewById(R.id.backButton)
        subtotalTextView = findViewById(R.id.subtotalPrice)
        deliveryTextView = findViewById(R.id.deliveryPrice)
        grandTotalTextView = findViewById(R.id.grandTotalPrice)
        deliveryPartnerNameTextView = findViewById(R.id.deliveryPartnerName)
        orderNoteTextView = findViewById(R.id.orderNote)
        thankYouButton = findViewById(R.id.thankYouBtn)

        // Initialize SharedPreferences and retrieve phone number
        sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        phoneNumber = sharedPreferences.getString("user_phone", "01234567890").toString()
        Log.d("ViewOrderActivity", "User Phone Number: $phoneNumber")

        // Set up click listeners
        backButton.setOnClickListener { finish() }
        thankYouButton.setOnClickListener { finish() }

        showLoadingState()

        // Fetch both product prices and then the specific order details

        fetchAndDisplayMostRecentOrder()

        // Set static text (can be updated dynamically if 'deliveryPartnerId' is fetched)
        deliveryPartnerNameTextView.text = "Our Delivery Partner"
        // orderNoteTextView.text is already set in XML
    }

    /**
     * Shows a loading message in the order summary section.
     */
    private fun showLoadingState() {
        itemsContainerLayout.removeAllViews()
        val loadingTextView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "Loading order details..."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }
        itemsContainerLayout.addView(loadingTextView)
        subtotalTextView.text = "Total: Calculating..."
        buildingNameTextView.text = "   Loading address..."
        locationNameTextView.text = "   "
    }

    /**
     * Fetches product prices and then the most recent order details for the customer.
     */
    private fun fetchAndDisplayMostRecentOrder() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Step 1: Fetch product prices first
                val url = "https://b2c-backend-eik4.onrender.com/api/v1/admin/getallproducts"
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder().url(url).build()
                val okHttpResponse = client.newCall(request).execute()

                if (okHttpResponse.isSuccessful) {
                    val responseBody = okHttpResponse.body?.string() ?: ""
                    if (responseBody.isNotEmpty()) {
                        val jsonArray = JSONArray(responseBody)
                        val tempProductPriceMap = mutableMapOf<String, Double>()
                        for (i in 0 until jsonArray.length()) {
                            val productObj = jsonArray.getJSONObject(i)
                            val productName = productObj.getString("name")
                            val price = productObj.getString("price").toDoubleOrNull() ?: productObj.getDouble("price")
                            tempProductPriceMap[productName] = price
                        }
                        productPriceMap = tempProductPriceMap
                        Log.d("ViewOrderActivity", "Product Price Map fetched: $productPriceMap")
                    } else {
                        Log.e("ViewOrderActivity", "Product API response body is empty.")
                    }
                } else {

                    Log.e("ViewOrderActivity", "Failed to fetch product details: ${okHttpResponse.code} - " + okHttpResponse.message)
                }
            } catch (e: Exception) {
                Log.e("ViewOrderActivity", "Exception fetching product details: ${e.message}", e)
            }

            // Step 2: Fetch the customer's previous orders
            RetrofitClient.apiService.getPreviousOrders(customerId = phoneNumber)
                .enqueue(object : Callback<OrderResponse> {
                    override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) { // This 'response' is retrofit2.Response
                        if (response.isSuccessful) {
                            response.body()?.let { orderResponse ->
                                Log.d("ViewOrderActivity", "Order History API Response: $orderResponse")
                                if (orderResponse.orders.isNotEmpty()) {
                                    // Find the most recent order based on createdAt timestamp
                                    val mostRecentOrder = orderResponse.orders.maxByOrNull { it.createdAt._seconds }
                                    if (mostRecentOrder != null) {
                                        Log.d("ViewOrderActivity", "Most recent order found: ${mostRecentOrder.id}")
                                        // Pass the found order and product prices for display
                                        displayOrderDetails(mostRecentOrder)
                                    } else {
                                        Log.w("ViewOrderActivity", "No recent order found despite non-empty list. (Timestamp issue?)")
                                        showNoOrderFound("No recent order found for this user.")
                                    }
                                } else {
                                    Log.d("ViewOrderActivity", "Order history is empty for this customer.")
                                    showNoOrderFound("No orders found in your history.")
                                }
                            } ?: run {
                                Log.e("ViewOrderActivity", "Order history response body is null.")
                                showNoOrderFound("Failed to load order history.")
                            }
                        } else {
                            // Using retrofit2.Response methods for logging
                            Log.e("ViewOrderActivity", "Failed to fetch order history: ${response.code()} - " + response.message())
                            Toast.makeText(this@ViewOrderActivity, "Failed to load order history.", Toast.LENGTH_LONG).show()
                            showNoOrderFound("Failed to load order history.")
                        }
                    }

                    override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                        Log.e("ViewOrderActivity", "Network error fetching order history: ${t.message}", t)
                        Toast.makeText(this@ViewOrderActivity, "Network error loading order history.", Toast.LENGTH_LONG).show()
                        showNoOrderFound("Network error loading order history. Check your connection.")
                    }
                })
        }
    }

    /**
     * Displays the details of a given OrderItem object on the UI.
     * It now uses the globally available 'productPriceMap'.
     */
    private fun displayOrderDetails(order: OrderItem) {
        val orderNumber = order.id.substringAfterLast("-")
        orderNoteTextView.text = "Order #$orderNumber"
        // --- Display Address Details ---
        // Retrieve the saved address JSON string from SharedPreferences
        val addressJson = sharedPreferences.getString("selected_address", null)
        if (addressJson != null) {
            try {
                val userAddress = Gson().fromJson(addressJson, UserAddress::class.java)
                buildingNameTextView.text = "   ${userAddress.fullAddress.flatNo}"
                locationNameTextView.text = "   ${userAddress.fullAddress.area}, ${userAddress.fullAddress.city}"
            } catch (e: Exception) {
                Log.e("ViewOrderActivity", "Error parsing saved address JSON for display", e)
                buildingNameTextView.text = "   Address data error"
                locationNameTextView.text = "   Please re-select address"
            }
        } else {
            // Fallback to order's address if no selected_address in SharedPreferences
            buildingNameTextView.text = "   ${order.orderAddress.flatNo}"
            locationNameTextView.text = "   ${order.orderAddress.area}, ${order.orderAddress.city}"
            Log.w("ViewOrderActivity", "No 'selected_address' in SharedPreferences, using order's address from backend.")
        }

        // --- Display Order Summary Items ---
        var totalCalculatedAmount = 0.0 // Calculate total from individual items + prices
        itemsContainerLayout.removeAllViews() // Clear any existing items or loading messages

        if (order.products.isNotEmpty()) {
            for ((productId, productDetail) in order.products) {
                // Get the display name from the backend product name
                val displayName = apiToDisplayNameMap[productDetail.name] ?: productDetail.name
                val itemQuantity = productDetail.quantity
                // Get the price using productDetail.name (e.g., "6pc_tray") from the fetched productPriceMap
                val individualPrice = productPriceMap[productDetail.name] ?: 0.0
                val individualItemTotal = individualPrice * itemQuantity
                totalCalculatedAmount += individualItemTotal

                Log.d("ViewOrderActivity", "Adding item to UI: $displayName (x$itemQuantity) @ ₹${individualPrice} = ₹${String.format("%.2f", individualItemTotal)}")

                val rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 4, 0, 4)
                    }
                }

                val nameQuantityTextView = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f
                    )
                    text = "$displayName (x$itemQuantity)"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setTextColor(ContextCompat.getColor(context, R.color.black))
                }

                val itemPriceTextView = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f
                    )
                    text = "₹${String.format("%.2f", individualItemTotal)}"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setTextColor(ContextCompat.getColor(context, R.color.black))
                    gravity = Gravity.END
                }

                rowLayout.addView(nameQuantityTextView)
                rowLayout.addView(itemPriceTextView)
                itemsContainerLayout.addView(rowLayout)
            }
        } else {
            Log.d("ViewOrderActivity", "Order.products list is empty for UI update.")
            val noItemsTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "No items found for this order."
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 0)
            }
            itemsContainerLayout.removeAllViews()
            itemsContainerLayout.addView(noItemsTextView)
        }

        // --- Display Total Price ---

        // --- Totals ---
        val subtotal = totalCalculatedAmount
        val deliveryCharge = 0.0 // (for now, always 0)
        val grandTotal = subtotal + deliveryCharge

        subtotalTextView.text = "Subtotal: ₹${String.format("%.2f", subtotal)}"
        deliveryTextView.text = "Delivery: ₹${String.format("%.2f", deliveryCharge)}"
        grandTotalTextView.text = "Grand Total: ₹${String.format("%.2f", grandTotal)}"

    }

    /**
     * Shows a message when no order details can be found.
     */
    private fun showNoOrderFound(message: String) {
        itemsContainerLayout.removeAllViews()
        val noOrderTextView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = message // Display the specific error message
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }
        itemsContainerLayout.addView(noOrderTextView)
        subtotalTextView.text = "Total: ₹0.00"
        buildingNameTextView.text = "   Order details not available"
        locationNameTextView.text = "   Please check order history"
    }
}