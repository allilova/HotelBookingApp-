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

    private var checkInMs: Long? = null
    private var checkOutMs: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getSharedPreferences("osm_pref", Context.MODE_PRIVATE))
        setContentView(R.layout.activity_hotel_detail)

        supportPostponeEnterTransition()

        val name      = intent.getStringExtra("HOTEL_NAME")      ?: "Хотел"
        val city      = intent.getStringExtra("HOTEL_CITY")      ?: ""
        val desc      = intent.getStringExtra("HOTEL_DESC")      ?: ""
        val imageUrl  = intent.getStringExtra("HOTEL_IMAGE")
        val lat       = intent.getDoubleExtra("HOTEL_LAT",       42.6977)
        val lon       = intent.getDoubleExtra("HOTEL_LON",       23.3219)
        val price     = intent.getDoubleExtra("HOTEL_PRICE",     0.0)
        val rating    = intent.getFloatExtra("HOTEL_RATING",     0f)
        val available = intent.getBooleanExtra("HOTEL_AVAILABLE", true)

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

        detailName.text = name
        detailDesc.text = desc
        tvPrice.text    = "%.2f лв. / нощ".format(price)
        rbRating.rating = rating
        tvAvailability.text = if (available) "✓ Свободен" else "✗ Зает"
        tvAvailability.setTextColor(
            if (available) getColor(R.color.teal_available) else getColor(R.color.red_unavailable)
        )


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


        btnPickDates.setOnClickListener {
            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build()
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Избери дати")
                .setCalendarConstraints(constraints)
                .build()
            picker.addOnPositiveButtonClickListener { selection ->
                checkInMs  = selection.first
                checkOutMs = selection.second
                val inStr  = sdf.format(Date(checkInMs!!))
                val outStr = sdf.format(Date(checkOutMs!!))
                tvDates.text = "Настаняване: $inStr  →  Напускане: $outStr"
            }
            picker.show(supportFragmentManager, "DATE_PICKER")
        }


        viewModel.loadFavouriteState(name.hashCode())
        lifecycleScope.launch {
            viewModel.isFavourite.collect { fav ->
                btnFav.setImageResource(
                    if (fav) android.R.drawable.btn_star_big_on
                    else     android.R.drawable.btn_star_big_off
                )
            }
        }
        btnFav.setOnClickListener {
            val fav = FavoriteHotel(id = name.hashCode(), name = name, city = city, imageUrl = imageUrl)
            viewModel.toggleFavourite(fav)
        }


        btnBook.setOnClickListener {
            if (checkInMs == null || checkOutMs == null) {
                Toast.makeText(this, "Моля, избери дати преди резервация.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val nights = ((checkOutMs!! - checkInMs!!) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
            val booking = Booking(
                hotelName     = name,
                hotelCity     = city,
                hotelImageUrl = imageUrl,
                checkIn       = sdf.format(Date(checkInMs!!)),
                checkOut      = sdf.format(Date(checkOutMs!!)),
                pricePerNight = price
            )
            viewModel.saveBooking(booking)
            sendNotification(name, nights, price)
            Toast.makeText(this, "Резервацията за $name е изпратена! ($nights нощ/и)", Toast.LENGTH_SHORT).show()
        }


        btnAR.setOnClickListener {
            startActivity(
                android.content.Intent(this, ARViewActivity::class.java)
                    .putExtra("HOTEL_NAME", name)
            )
        }
    }

    private fun sendNotification(hotelName: String, nights: Long, price: Double) {
        val channelId = "hotel_notifications"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Booking Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val total = nights * price
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Успешна резервация!")
            .setContentText("$hotelName — $nights нощ/и — %.2f лв.".format(total))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            return
        }
        nm.notify(1, builder.build())
    }
}