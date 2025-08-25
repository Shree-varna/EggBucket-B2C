package com.eggbucket.eggbucket_b2c

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class Product1Fragment : Fragment() {

    private val images = listOf(R.drawable.eggs_image_12, R.drawable.eggside, R.drawable.egg_12_back)
    private lateinit var sharedPreferences: SharedPreferences

    private var count2 = 0
    private var dynamicPrice: Double = 0.0 // Fixed price for pack of 12
    private lateinit var viewPager: ViewPager2
    private val handler = Handler(Looper.getMainLooper())
    private var currentPage = 0
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        sharedPreferences = requireActivity().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        loadQuantityFromSharedPreferences()

        val view = inflater.inflate(R.layout.fragment_product1, container, false)
        viewPager = view.findViewById(R.id.imageCarousel)
        val carouselAdapter = CarouselAdapterProduct(images)
        viewPager.adapter = carouselAdapter
        bottomNavigationView= requireActivity().findViewById(R.id.nav_view)
        setupCard2Listeners(view)
        setupUI(view)
        startAutoScroll(view)

        fetchProductPrice()

        return view
    }

    private fun setupCard2Listeners(view: View) {
        val decreaseBut2 = view.findViewById<ImageButton>(R.id.decreaseButton2)
        val increaseBut2 = view.findViewById<ImageButton>(R.id.increaseButton2)
        val quantityText2 = view.findViewById<TextView>(R.id.quantityText2)

        decreaseBut2.setOnClickListener {
            if (count2 > 0) {
                count2--
                quantityText2.text = count2.toString()
                updatePrice(view)
                saveQuantityToSharedPreferences()
            }
        }

        increaseBut2.setOnClickListener {
            count2++
            quantityText2.text = count2.toString()
            updatePrice(view)
            saveQuantityToSharedPreferences()
        }
    }

    private fun updatePrice(view: View) {
        val priceTextView = view.findViewById<TextView>(R.id.productprize)
        val priceSectionTextView = view.findViewById<TextView>(R.id.priceText)
        val totalPrice = dynamicPrice * count2
        val formattedPrice = "₹${"%.2f".format(totalPrice)}"
        val packprice = "₹${"%.2f".format(dynamicPrice)}/pack"
        priceTextView.text = packprice
        priceSectionTextView?.text = formattedPrice
    }

    private fun saveQuantityToSharedPreferences() {
        with(sharedPreferences.edit()) {
            putInt("count2", count2)
            apply()
        }
    }

    private fun loadQuantityFromSharedPreferences() {
        count2 = sharedPreferences.getInt("count2", 0)
    }

    private fun startAutoScroll(view: View) {
        val indi1 = view.findViewById<ImageView>(R.id.indi1)
        val indi2 = view.findViewById<ImageView>(R.id.indi2)
        val indi3 = view.findViewById<ImageView>(R.id.indi3)

        val runnable = object : Runnable {
            override fun run() {
                if (currentPage == images.size) {
                    currentPage = 0
                }
                viewPager.setCurrentItem(currentPage, true)
                updateIndicatorBackgrounds(indi1, indi2, indi3, currentPage)
                currentPage++
                handler.postDelayed(this, 5000)
            }
        }
        handler.postDelayed(runnable, 5000)
    }

    private fun updateIndicatorBackgrounds(ind1: ImageView, ind2: ImageView, ind3: ImageView, currentPage: Int) {
        ind1.setBackgroundResource(R.drawable.indicator_inactive)
        ind2.setBackgroundResource(R.drawable.indicator_inactive)
        ind3.setBackgroundResource(R.drawable.indicator_inactive)

        when (currentPage) {
            0 -> ind1.setBackgroundResource(R.drawable.indicator_active)
            1 -> ind2.setBackgroundResource(R.drawable.indicator_active)
            2 -> ind3.setBackgroundResource(R.drawable.indicator_active)
        }
    }

    override fun onResume() {
        super.onResume()
        loadQuantityFromSharedPreferences()
        val quantityText2 = view?.findViewById<TextView>(R.id.quantityText2)
        quantityText2?.text = count2.toString()
        updatePrice(requireView())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }

    private fun setupUI(view: View) {
        val previousButton = view.findViewById<ImageButton>(R.id.backButton)
        previousButton.setOnClickListener {
            findNavController().navigate(R.id.action_product1Fragment_to_navigation_home)
        }

        val addToCart = view.findViewById<Button>(R.id.addToCartButton)
        addToCart.setOnClickListener {
            if (count2 == 0) {
                Snackbar.make(view, "Add Items to Cart!", Snackbar.LENGTH_SHORT).show()
            } else {
                bottomNavigationView.selectedItemId = R.id.navigation_cart
            }
        }
    }
    private fun fetchProductPrice() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = "https://b2c-backend-eik4.onrender.com/api/v1/admin/getallproducts"
                // Optionally, use a shared OkHttpClient with timeout settings if available
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val jsonArray = JSONArray(responseBody)
                    for (i in 0 until jsonArray.length()) {
                        val productObj = jsonArray.getJSONObject(i)
                        val productName = productObj.getString("name")
                        if (productName == "12pc_tray") {
                            // Use currentPrice if available; fallback to "price" otherwise.
                            val priceStr = productObj.getString("price")
                            dynamicPrice = priceStr.toDoubleOrNull() ?: productObj.getDouble("price")
                            break
                        }
                    }
                    withContext(Dispatchers.Main) {
                        // Safely update UI only if view is still available
                        view?.let { updatePrice(it) }
                    }
                } else {
                    Log.e("ProductAPI", "Failed to fetch product details: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("ProductAPI", "Exception in fetching product details", e)
            }
        }
    }
}