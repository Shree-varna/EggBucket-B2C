package com.eggbucket.eggbucket_b2c

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp as FireBaseTimestamp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class OrderHistoryAdapter(private val orderList: List<OrderItem>) :
    RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder>() {

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            notifyDataSetChanged()
            handler.postDelayed(this, 60 * 1000) // Refresh every minute
        }
    }

    init {
        handler.postDelayed(updateRunnable, 60 * 1000)
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderDate: TextView = itemView.findViewById(R.id.orderDate)
        val orderAmt: TextView = itemView.findViewById(R.id.orderAmt)
        val items: TextView = itemView.findViewById(R.id.items)
        val deliveryStatus: TextView = itemView.findViewById(R.id.orderId)
        val cancelOrderBtn: Button = itemView.findViewById(R.id.cancel_order_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.order_item, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orderList[position]
        val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        // Format the date
        holder.orderDate.text = "Order At: " + sdf.format(
            Date(
                (order.createdAt._seconds * 1000) + (order.createdAt._nanoseconds / 1000000)
            )
        )

        holder.orderAmt.text = "Order Amount: ₹${order.amount}"

        // 1) Map product IDs to a friendly name
        val productNameMap = mapOf(
            "0Xkt5nPNGubaZ9mMpzGs" to "Eggs x 6",
            "NVPDbCfymcyD7KpH6J5J" to "Eggs x 12",
            "a2MeuuaCweGQNBIc4l51" to "Eggs x 30"
        )

        // 2) Build a string with just the friendly name and integer quantity
        val itemStringBuilder = StringBuilder()
        order.products.forEach { (productId, productDetail) ->
            val displayName = productNameMap[productId] ?: "Unknown Product"
            itemStringBuilder.append("$displayName: ${productDetail.quantity}\n")
        }
        holder.items.text = itemStringBuilder.toString().trim()

        // Show order ID and status
        val shortId = if (order.id.length >= 3) order.id.takeLast(3) else order.id
        holder.deliveryStatus.text = "Order ID: $shortId\nStatus: ${order.status}"


        // Enable or disable Cancel button
        if (getTimeDifference(
                FireBaseTimestamp(order.createdAt._seconds, order.createdAt._nanoseconds.toInt())
            ) < 10L && order.status == "Pending"
        ) {
            holder.cancelOrderBtn.visibility = View.VISIBLE
            holder.cancelOrderBtn.setOnClickListener {
                cancelOrder(order.id, holder)
            }
        } else {
            holder.cancelOrderBtn.visibility = View.GONE
        }

        // Color-code the status
        when {
            order.status.equals("Canceled", ignoreCase = true) -> {
                holder.deliveryStatus.setTextColor(Color.RED)
            }
            order.status.equals("Pending", ignoreCase = true) -> {
                holder.deliveryStatus.setTextColor(Color.BLACK)
            }
            else -> {
                holder.deliveryStatus.setTextColor(Color.GREEN)
            }
        }
    }



    fun getTimeDifference(createdAt: FireBaseTimestamp): Long {
        val currentTime = FireBaseTimestamp.now()
        val diffInMillis = (currentTime.seconds - createdAt.seconds) * 1000L
        return TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
    }

    private fun cancelOrder(orderId: String, holder: OrderViewHolder) {
        RetrofitClient.apiService.cancelOrder(orderId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("OrderCancel", "Order $orderId canceled successfully")
                    holder.cancelOrderBtn.visibility = View.GONE
                    holder.deliveryStatus.text = "Order ID: $orderId\nStatus: Canceled"
                } else {
                    Log.e("OrderCancel", "Failed to cancel order: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("OrderCancel", "API call failed: ${t.message}")
            }
        })
    }

    override fun getItemCount(): Int = orderList.size

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        handler.removeCallbacks(updateRunnable)
    }
}
