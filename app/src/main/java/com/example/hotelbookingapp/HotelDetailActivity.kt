package com.example.hotelbookingapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class HotelDetailActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getSharedPreferences("osm_pref", Context.MODE_PRIVATE))
        setContentView(R.layout.activity_hotel_detail)


        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "hotel-db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()


        val name     = intent.getStringExtra("HOTEL_NAME")  ?: "Хотел"
        val city     = intent.getStringExtra("HOTEL_CITY")  ?: "Непознат град"
        val desc     = intent.getStringExtra("HOTEL_DESC")  ?: "Няма описание"
        val imageUrl = intent.getStringExtra("HOTEL_IMAGE")
        val lat      = intent.getDoubleExtra("HOTEL_LAT", 42.6977)
        val lon      = intent.getDoubleExtra("HOTEL_LON", 23.3219)


        val map = findViewById<org.osmdroid.views.MapView>(R.id.map)
        map.setMultiTouchControls(true)
        val hotelLocation = GeoPoint(lat, lon)
        map.controller.setZoom(15.0)
        map.controller.setCenter(hotelLocation)
        val marker = Marker(map)
        marker.position = hotelLocation
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = name
        map.overlays.add(marker)


        findViewById<TextView>(R.id.detailName).text = name
        findViewById<TextView>(R.id.detailDescription).text = desc
        Glide.with(this).load(imageUrl).into(findViewById<ImageView>(R.id.detailImage))


        findViewById<Button>(R.id.btnBookNow).setOnClickListener {
            sendNotification(name)
            Toast.makeText(this, "Резервацията за $name е изпратена!", Toast.LENGTH_SHORT).show()
        }


        findViewById<Button>(R.id.btnViewAR).setOnClickListener {
            startActivity(
                android.content.Intent(this, ARViewActivity::class.java)
                    .putExtra("HOTEL_NAME", name)
            )
        }


        val btnFavorite = findViewById<ImageButton>(R.id.btnFavorite)

        lifecycleScope.launch {
            val isFav = withContext(Dispatchers.IO) {
                db.hotelDao().getFavoriteById(name.hashCode()) != null
            }
            if (isFav) btnFavorite.setImageResource(android.R.drawable.btn_star_big_on)
        }

        btnFavorite.setOnClickListener {
            lifecycleScope.launch {
                val existing = withContext(Dispatchers.IO) {
                    db.hotelDao().getFavoriteById(name.hashCode())
                }
                if (existing == null) {
                    val favHotel = FavoriteHotel(
                        id = name.hashCode(),
                        name = name,
                        city = city,
                        imageUrl = imageUrl
                    )
                    withContext(Dispatchers.IO) { db.hotelDao().insertFavorite(favHotel) }
                    btnFavorite.setImageResource(android.R.drawable.btn_star_big_on)
                    Toast.makeText(this@HotelDetailActivity, "$name е добавен в Любими!", Toast.LENGTH_SHORT).show()
                } else {
                    withContext(Dispatchers.IO) { db.hotelDao().deleteFavorite(existing) }
                    btnFavorite.setImageResource(android.R.drawable.btn_star_big_off)
                    Toast.makeText(this@HotelDetailActivity, "$name е премахнат от Любими!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendNotification(hotelName: String) {
        val channelId = "hotel_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Booking Notifications", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Успешна резервация!")
            .setContentText("Вашата стая в $hotelName е запазена.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101
                )
                return
            }
        }

        notificationManager.notify(1, builder.build())
    }
}