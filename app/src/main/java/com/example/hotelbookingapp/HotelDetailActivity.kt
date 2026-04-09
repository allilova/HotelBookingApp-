package com.example.hotelbookingapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val locationViewModel: LocationViewModel by viewModels()
    private companion object { const val REQUEST_LOCATION = 300 }

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
        val hotelId   = intent.getIntExtra("HOTEL_ID", -1)
        val price     = intent.getDoubleExtra("HOTEL_PRICE",     0.0)
        val rating    = intent.getFloatExtra("HOTEL_RATING",     0f)
        val available = intent.getBooleanExtra("HOTEL_AVAILABLE", true)
        val lat       = intent.getDoubleExtra("HOTEL_LAT",       42.6977)
        val lon       = intent.getDoubleExtra("HOTEL_LON",       23.3219)
        val imageUrl  = intent.getStringExtra("HOTEL_IMAGE")

        // ── View references ──────────────────────────────────────────────────
        val detailImage    = findViewById<ImageView>(R.id.detailImage)
        val detailName     = findViewById<TextView>(R.id.detailName)
        val detailDesc     = findViewById<TextView>(R.id.detailDescription)
        val tvPrice        = findViewById<TextView>(R.id.detailPrice)
        val rbRating       = findViewById<RatingBar>(R.id.detailRating)
        val tvAvailability = findViewById<TextView>(R.id.detailAvailability)
        val tvDates        = findViewById<TextView>(R.id.tvSelectedDates)
        val tvDistance     = findViewById<TextView>(R.id.tvDistance)
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

        // ── Basic UI (price/rating/availability don't need DB) ───────────────
        tvPrice.text    = getString(R.string.price_per_night, price)
        rbRating.rating = rating
        tvAvailability.text = getString(if (available) R.string.available else R.string.unavailable)
        tvAvailability.setTextColor(
            if (available) getColor(R.color.teal_available)
            else           getColor(R.color.red_unavailable)
        )

        // ── Resolve hotel name/city/description on IO thread (FIXES CRASH) ───
        lifecycleScope.launch {
            val hotel = withContext(Dispatchers.IO) { resolveHotel(hotelId) }
            val name  = hotel?.name        ?: ""
            val city  = hotel?.city        ?: ""
            val desc  = hotel?.description ?: ""

            detailName.text = name
            detailDesc.text = desc

            // ── Map ──────────────────────────────────────────────────────────
            setupMap(lat, lon, name)

            // ── Favourite ────────────────────────────────────────────────────
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
                        name     = name,
                        city     = city,
                        imageUrl = imageUrl
                    )
                )
            }

            // ── Booking ──────────────────────────────────────────────────────
            btnBook.setOnClickListener {
                if (checkInMs == null || checkOutMs == null) {
                    Toast.makeText(this@HotelDetailActivity,
                        getString(R.string.pick_dates_first), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.saveBooking(
                    Booking(
                        hotelId       = hotelId,
                        hotelName     = name,
                        hotelCity     = city,
                        hotelImageUrl = imageUrl,
                        checkIn       = sdf.format(Date(checkInMs!!)),
                        checkOut      = sdf.format(Date(checkOutMs!!)),
                        pricePerNight = price
                    )
                )
            }

            // ── AR ───────────────────────────────────────────────────────────
            btnAR.setOnClickListener {
                startActivity(android.content.Intent(this@HotelDetailActivity, ARViewActivity::class.java)
                    .putExtra("HOTEL_NAME", name)
                    .putExtra("HOTEL_LAT",  lat)
                    .putExtra("HOTEL_LON",  lon))
            }
        }

        // ── Observe booking result ───────────────────────────────────────────
        lifecycleScope.launch {
            viewModel.bookingSaved.collect { saved ->
                if (saved) {
                    val nights = ((checkOutMs!! - checkInMs!!) / (1000 * 60 * 60 * 24))
                        .coerceAtLeast(1)
                    // Get name for notification — read from UI
                    val name = findViewById<TextView>(R.id.detailName).text.toString()
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

        // ── Location ─────────────────────────────────────────────────────────
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationViewModel.startTracking(this)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION
            )
        }
        lifecycleScope.launch {
            locationViewModel.location.collect { userLoc ->
                if (userLoc == null) return@collect
                val km = locationViewModel.distanceKm(
                    userLoc.latitude, userLoc.longitude, lat, lon
                )
                tvDistance.text = getString(R.string.distance_from_you, km)
            }
        }
    }

    /**
     * MUST be called from a background thread (IO dispatcher).
     * Resolves a Hotel object for any hotel ID — both static (1–999) and
     * custom / host-created (>= 1000).
     */
    private fun resolveHotel(hotelId: Int): Hotel? {
        return if (HotelRepository.isCustomId(hotelId)) {
            val db = DatabaseProvider.get(this)
            val customId = HotelRepository.hotelIdToCustomId(hotelId)
            db.customHotelDao().getHotelById(customId)?.let {
                with(HotelRepository) { it.toHotel() }
            }
        } else {
            HotelRepository.getStaticHotels(this).find { it.id == hotelId }
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            locationViewModel.startTracking(this)
        }
    }

    override fun onStop()  { super.onStop();  locationViewModel.stopTracking() }
    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationViewModel.startTracking(this)
        }
    }
}