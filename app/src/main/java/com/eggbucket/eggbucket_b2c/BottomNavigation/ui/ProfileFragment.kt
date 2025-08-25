package com.eggbucket.eggbucket_b2c.BottomNavigation.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.eggbucket.eggbucket_b2c.R
import com.eggbucket.eggbucket_b2c.databinding.FragmentProfileBinding
import com.eggbucket.eggbucket_b2c.uiscreens.LoginWithOtpActivity

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPref: SharedPreferences
    private lateinit var number: SharedPreferences
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        // Initialize SharedPreferences
        sharedPref = requireActivity().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        // Fetch details from SharedPreferences
         val name = sharedPref.getString("name", "Please update Profile ! ")
        val phone = sharedPref.getString("user_phone", null) // null if not logged in

        // Update UI with fetched details
        binding.personName.text = "$name"
        binding.phoneNo.text = "$phone"
        // Set up click listeners
        setupClickListeners()

        return binding.root
    }

    private fun setupClickListeners() {
        // Navigate to Address Fragment
        binding.addressesLayout.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_notifications_to_addressListFragment)
        }

        // Navigate to Order History
        binding.yourOrdersLayout.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_notifications_to_orderHistory)
        }

        // Navigate to Edit Profile Fragment
        binding.editProfileLayout.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_notifications_to_editProfile)
        }
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.notification.setOnClickListener{
            findNavController().navigate(R.id.action_navigation_notifications_to_notificationF)
        }

        // Handle Logout
        binding.logoutLayout.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        // Clear SharedPreferences
        val editor = sharedPref.edit()
        editor.clear()
        editor.apply()

        // Redirect to Login Activity
        val intent = Intent(requireActivity(), LoginWithOtpActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) // Clear activity stack
        startActivity(intent)

        // Finish current activity
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
