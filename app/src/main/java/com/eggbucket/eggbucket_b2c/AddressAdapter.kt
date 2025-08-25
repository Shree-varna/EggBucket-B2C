package com.eggbucket.eggbucket_b2c

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AddressAdapter(
    private var addresses: List<UserAddress>,
    private val onDeleteClick: (Int) -> Unit,
    private val onAddressClick: (UserAddress) -> Unit,
    private val onEditClick: (UserAddress, Int) -> Unit
) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    // CHANGE 1: ViewHolder now holds references to all the new TextViews
    class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flatNo: TextView = itemView.findViewById(R.id.textViewFlatNo)
        val addressLine1: TextView = itemView.findViewById(R.id.textViewAddressLine1)
        val areaCity: TextView = itemView.findViewById(R.id.textViewAreaCity)
        val stateZipCountry: TextView = itemView.findViewById(R.id.textViewStateZipCountry)
        val deleteButton: ImageView = itemView.findViewById(R.id.delete_button)
        val editButton: ImageView = itemView.findViewById(R.id.edit_address)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_address_list, parent, false)
        return AddressViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val address = addresses[position]

        // CHANGE 2: Populate all the new TextViews with data from the address object
        holder.flatNo.text = address.fullAddress.flatNo ?: ""
        holder.addressLine1.text = address.fullAddress.addressLine1 ?: ""

        // Combine fields for a cleaner display
        holder.areaCity.text = "${address.fullAddress.area ?: ""}, ${address.fullAddress.city ?: ""}"
        holder.stateZipCountry.text = "${address.fullAddress.state ?: ""} - ${address.fullAddress.zipCode ?: ""}, ${address.fullAddress.country ?: ""}"

        holder.deleteButton.setOnClickListener {
            Log.d("AddressAdapter", "Delete clicked for position: $position")
            onDeleteClick(position)
        }

        // CHANGE 3: The click listener is on the whole item for a better user experience
        holder.itemView.setOnClickListener {
            onAddressClick(address)
        }

        holder.editButton.setOnClickListener {
            onEditClick(addresses[position], position)
        }
    }

    override fun getItemCount(): Int = addresses.size
}