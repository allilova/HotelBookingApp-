package com.example.hotelbookingapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.*

class HotelDetailActivity : AppCompatActivity() {

    private val viewModel: HotelDetailViewModel by viewModels()
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var checkInMs: Long?  = null
    private var checkOutMs: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Configuration.getInstance()
                .load(this, getSharedPreferences("osm_pref", Context.MODE_PRIVATE))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_map_config), Toast.LENGTH_SHORT).show()
        }

        setContentView(R.layout.activity_hotel_detail)
        supportPostponeEnterTransition()

        // Only non-translatable data comes via Intent.
        // Translatable strings (name, city, desc) are resolved here using the
        // active locale context so they are always in the correct language.
        val hotelId   = intent.getIntExtra("HOTEL_ID", -1)
        val price     = intent.getDoubleExtra("HOTEL_PRICE",     0.0)
        val rating    = intent.getFloatExtra("HOTEL_RATING",     0f)
        val available = intent.getBooleanExtra("HOTEL_AVAILABLE", true)
        val lat       = intent.getDoubleExtra("HOTEL_LAT",       42.6977)
        val lon       = intent.getDoubleExtra("HOTEL_LON",       23.3219)
        val imageUrl  = intent.getStringExtra("HOTEL_IMAGE")

        val hotel = HotelRepository.getHotels(this).find { it.id == hotelId }
        val name  = hotel?.name        ?: ""
        val city  = hotel?.city        ?: ""
        val desc  = hotel?.description ?: ""

        // ── View references ──────────────────────────────────────────────────
        val detailImage    = findViewById<ImageView>(R.id.detailImage)
        val detailName     = findViewById<TextView>(R.id.detailName)
        val detailDesc     = findViewById<TextView>(R.id.detailDescription)
        val tvPrice        = findViewById<TextView>(R.id.detailPrice)
        val rbRating       = findViewById<RatingBar>(R.id.detailRating)
        val tvAvailability = findViewById<TextView>(R.id.detailAvailability)
        val tvDates        = findViewById<TextView>(R.id.tvSelectedDates)
        val btnPickDates   = findViewById<Button>(R.id.btnPickDates)
        val btnBook        = findViewById<Button>(R.id.btnBookNow)
        val btnAR          = findViewById<Button>(R.id.btnViewAR)
        val btnFav         = findViewById<ImageButton>(R.id.btnFavorite)

        detailImage.transitionName = "hotelImageTransition"

        // ── Image ────────────────────────────────────────────────────────────
        Glide.with(this)
            .load(imageUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_launcher_background)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?,
                    target: Target<Drawable>, isFirstResource: Boolean
                ): Boolean {
                    supportStartPostponedEnterTransition()
                    Toast.makeText(this@HotelDetailActivity,
                        getString(R.string.error_image_load), Toast.LENGTH_SHORT).show()
                    return false
                }
                override fun onResourceReady(
                    resource: Drawable, model: Any,
                    target: Target<Drawable>?, dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    supportStartPostponedEnterTransition()
                    return false
                }
            })
            .into(detailImage)

        // ── Basic UI ─────────────────────────────────────────────────────────
        detailName.text = name
        detailDesc.text = desc
        tvPrice.text    = getString(R.string.price_per_night, price)
        rbRating.rating = rating
        tvAvailability.text = getString(if (available) R.string.available else R.string.unavailable)
        tvAvailability.setTextColor(
            if (available) getColor(R.color.teal_available)
            else           getColor(R.color.red_unavailable)
        )

        // ── Map ──────────────────────────────────────────────────────────────
        setupMap(lat, lon, name)

        // ── Date picker ──────────────────────────────────────────────────────
        btnPickDates.setOnClickListener {
            try {
                val constraints = CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.now())
                    .build()
                val picker = MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText(getString(R.string.date_picker_title))
                    .setCalendarConstraints(constraints)
                    .build()
                picker.addOnPositiveButtonClickListener { selection ->
                    checkInMs  = selection.first
                    checkOutMs = selection.second
                    tvDates.text = getString(R.string.selected_dates,
                        sdf.format(Date(checkInMs!!)),
                        sdf.format(Date(checkOutMs!!)))
                }
                picker.show(supportFragmentManager, "DATE_PICKER")
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.error_date_picker),
                    Toast.LENGTH_SHORT).show()
            }
        }

        // ── Favourite ────────────────────────────────────────────────────────
        viewModel.loadFavouriteState(hotelId)

        lifecycleScope.launch {
            viewModel.isFavourite.collect { fav ->
                btnFav.setImageResource(
                    if (fav) android.R.drawable.btn_star_big_on
                    else     android.R.drawable.btn_star_big_off
                )
            }
        }

        btnFav.setOnClickListener {
            viewModel.toggleFavourite(
                FavoriteHotel(
                    id       = hotelId,
                    hotelId  = hotelId,
                    name     = name,     // stored as fallback only
                    city     = city,     // stored as fallback only
                    imageUrl = imageUrl
                )
            )
        }

        // ── Booking ──────────────────────────────────────────────────────────
        btnBook.setOnClickListener {
            if (checkInMs == null || checkOutMs == null) {
                Toast.makeText(this, getString(R.string.pick_dates_first),
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.saveBooking(
                Booking(
                    hotelId       = hotelId,
                    hotelName     = name,     // stored as fallback only
                    hotelCity     = city,     // stored as fallback only
                    hotelImageUrl = imageUrl,
                    checkIn       = sdf.format(Date(checkInMs!!)),
                    checkOut      = sdf.format(Date(checkOutMs!!)),
                    pricePerNight = price
                )
            )
        }

        // ── Observe booking result ───────────────────────────────────────────
        lifecycleScope.launch {
            viewModel.bookingSaved.collect { saved ->
                if (saved) {
                    val nights = ((checkOutMs!! - checkInMs!!) / (1000 * 60 * 60 * 24))
                        .coerceAtLeast(1)
                    sendNotification(name, nights, price)
                    Toast.makeText(this@HotelDetailActivity,
                        getString(R.string.booking_sent, name, nights),
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ── Observe errors ───────────────────────────────────────────────────
        lifecycleScope.launch {
            viewModel.errorEvent.collect { message ->
                Toast.makeText(this@HotelDetailActivity, message, Toast.LENGTH_LONG).show()
            }
        }

        // ── AR ───────────────────────────────────────────────────────────────
        btnAR.setOnClickListener {
            startActivity(android.content.Intent(this, ARViewActivity::class.java)
                .putExtra("HOTEL_NAME", name))
        }
    }

    private fun setupMap(lat: Double, lon: Double, name: String) {
        try {
            val map = findViewById<org.osmdroid.views.MapView>(R.id.map)
            map.setMultiTouchControls(true)
            val location = GeoPoint(lat, lon)
            map.controller.setZoom(15.0)
            map.controller.setCenter(location)
            Marker(map).apply {
                position = location
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = name
                map.overlays.add(this)
            }
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_map_load), Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendNotification(hotelName: String, nights: Long, price: Double) {
        try {
            val channelId = "hotel_notifications"
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(NotificationChannel(
                    channelId, getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT))
            }
            val total   = nights * price
            val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_text, hotelName, nights, total))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                return
            }
            nm.notify(1, builder.build())
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_notification), Toast.LENGTH_SHORT).show()
        }
    }
}