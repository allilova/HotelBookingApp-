package com.example.hotelbookingapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MyHotelsActivity : AppCompatActivity() {

    private lateinit var adapter: MyHotelsAdapter

    private val addHotelLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) refreshData()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_hotels)

        supportActionBar?.apply {
            title = getString(R.string.title_my_hotels)
            setDisplayHomeAsUpEnabled(true)
        }

        val rv      = findViewById<RecyclerView>(R.id.rvMyHotels)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyMyHotels)
        val fab     = findViewById<FloatingActionButton>(R.id.fabAddHotelFromMyHotels)

        rv.layoutManager = LinearLayoutManager(this)

        adapter = MyHotelsAdapter(
            list     = emptyList(),
            onDelete = { hotel -> confirmDelete(hotel) }
        )
        rv.adapter = adapter

        fab.setOnClickListener {
            addHotelLauncher.launch(Intent(this, AddHotelActivity::class.java))
        }

        refreshData()
    }

    /**
     * Loads the current host's hotels from Firestore.
     * Uses FirebaseAuthManager.currentUid to identify the host.
     */
    private fun refreshData() {
        val uid = FirebaseAuthManager.currentUid
        if (uid == null) return

        val rv      = findViewById<RecyclerView>(R.id.rvMyHotels)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyMyHotels)

        lifecycleScope.launch {
            try {
                // Query Firestore for hotels WHERE ownerUserId == currentUid
                val hotels = CustomHotelRepository.getHotelsByOwner(uid)

                if (hotels.isEmpty()) {
                    rv.visibility      = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    rv.visibility      = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                    adapter.updateData(hotels)
                }
            } catch (e: Exception) {
                rv.visibility      = View.GONE
                tvEmpty.visibility = View.VISIBLE
                Toast.makeText(
                    this@MyHotelsActivity,
                    getString(R.string.delete_hotel_error),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Shows a confirmation dialog before deleting a hotel.
     * If confirmed, deletes the hotel from Firestore by its firestoreId.
     */
    private fun confirmDelete(hotel: CustomHotel) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_hotel_confirm_title))
            .setMessage(getString(R.string.delete_hotel_confirm_msg, hotel.name))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                lifecycleScope.launch {
                    try {
                        // Delete by Firestore document ID — no Room involved
                        CustomHotelRepository.deleteHotel(hotel.firestoreId)
                        Toast.makeText(
                            this@MyHotelsActivity,
                            getString(R.string.delete_hotel_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        refreshData()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MyHotelsActivity,
                            getString(R.string.delete_hotel_error),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────

/**
 * Adapter for the host's "My Hotels" list.
 * Shows hotel name, city, price, and a delete button.
 * Now works with CustomHotel from Firestore instead of Room.
 */
class MyHotelsAdapter(
    private var list: List<CustomHotel>,
    private val onDelete: (CustomHotel) -> Unit
) : RecyclerView.Adapter<MyHotelsAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName:    TextView    = view.findViewById(R.id.myHotelName)
        val tvCity:    TextView    = view.findViewById(R.id.myHotelCity)
        val tvPrice:   TextView    = view.findViewById(R.id.myHotelPrice)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteMyHotel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_hotel, parent, false))

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val h = list[position]
        holder.tvName.text  = h.name
        holder.tvCity.text  = h.city
        holder.tvPrice.text = "${h.price} BGN / нощ"
        holder.btnDelete.setOnClickListener { onDelete(h) }
    }

    fun updateData(newList: List<CustomHotel>) {
        list = newList
        notifyDataSetChanged()
    }
}