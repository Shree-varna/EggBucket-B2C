package com.eggbucket.eggbucket_b2c

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.findNavController
import nl.dionsegijn.konfetti.xml.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position


import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

class OrderCompleted : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_order_completed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🎉 Trigger konfetti when fragment loads
        val konfettiView = view.findViewById<KonfettiView>(R.id.konfettiView)

        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            position = Position.Relative(0.5, 0.0),
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(50),
        )

        konfettiView.start(party)

        // ✅ Buttons
        val gotoHomeButton: Button = view.findViewById(R.id.gotoHome)
        gotoHomeButton.setOnClickListener {
            it.findNavController().navigate(R.id.action_orderCompleted_to_navigation_home)
        }

        val viewOrdersButton: Button = view.findViewById(R.id.gotoViewOrder)
        viewOrdersButton.setOnClickListener {
            val intent = Intent(requireContext(), ViewOrderActivity::class.java)
            startActivity(intent)
        }

        val recipePageButton: Button = view.findViewById(R.id.gotoRecipes)
        recipePageButton.setOnClickListener {
            val intent = Intent(requireContext(), RecipeActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = OrderCompleted()
    }
}
