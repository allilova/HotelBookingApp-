# HotelBookingApp 🏨

An Android hotel booking application built with Kotlin, Firebase, and Material Design 3. Users can browse hotels, make reservations, manage favourites, and experience AR navigation toward hotel locations.

---

## Features

### For Guests
- **Browse hotels** — static listings plus host-created hotels fetched from Firestore
- **Search & sort** — filter by name/city; sort by price (asc/desc) or rating
- **Hotel detail** — hero image with shared-element transition, OSMDroid map, distance from current location
- **Date picker** — Material date range picker with past-date validation
- **Multi-guest booking** — dynamic guest name fields (up to 10 guests)
- **Booking history** — view all past bookings with status badges (Pending / Confirmed / Cancelled)
- **Cancel bookings** — guests can cancel Pending or Confirmed bookings
- **Favourites** — save/remove hotels; stored locally in Room DB
- **AR view** — camera passthrough with a compass arrow pointing toward the hotel, live distance label, and cardinal direction
- **Shake to shuffle** — shake the device to randomise the hotel list
- **Bonus points** — VIP toast shown when a user reaches 100+ points
- **Dark mode** — toggle between light and dark themes (persisted across sessions)
- **Language switcher** — Bulgarian / English (persisted across sessions)

### For Hosts
- **Add hotels** — form with name, city, price, description, image URL, and GPS coordinates
- **My Hotels** — list of all hotels the host has created, with delete confirmation
- **Booking requests** — view all incoming bookings for their hotels, with Confirm / Cancel actions
- **Pending count badge** — summary card showing how many bookings await action

### Notifications
- **Firestore-based push** — no deprecated FCM Legacy API; notifications are written to a `notifications` collection and read in real time by `NotificationListenerService`
- Guest notified when host confirms or cancels a booking
- Host notified when a guest makes or cancels a booking

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Material Design 3, ConstraintLayout, RecyclerView |
| Architecture | MVVM — ViewModel + StateFlow + SharedFlow |
| Auth | Firebase Authentication |
| Remote DB | Firebase Firestore |
| Local DB | Room (favourites only) |
| Image loading | Glide |
| Maps | OSMDroid |
| Camera / AR | CameraX |
| Location | FusedLocationProviderClient (fallback: LocationManager) |
| Notifications | Firestore listener + NotificationCompat |
| Build system | Gradle (Kotlin DSL), AGP 9.1, KSP 2.3.5 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

---

## Project Structure

```
app/src/main/java/com/example/hotelbookingapp/
├── activities/
│   ├── SplashActivity.kt
│   ├── LoginActivity.kt
│   ├── RegisterActivity.kt
│   ├── MainActivity.kt
│   ├── HotelDetailActivity.kt
│   ├── FavoritesActivity.kt
│   ├── BookingHistoryActivity.kt
│   ├── UserProfileActivity.kt
│   ├── AddHotelActivity.kt
│   ├── MyHotelsActivity.kt
│   ├── HostBookingsActivity.kt
│   └── ARViewActivity.kt
├── adapters/
│   ├── HotelAdapter.kt
│   ├── FavoriteAdapter.kt
│   ├── BookingAdapter.kt
│   └── HostBookingAdapter.kt
├── data/
│   ├── Hotel.kt
│   ├── CustomHotel.kt
│   ├── FavoriteHotel.kt
│   ├── Booking.kt
│   └── User.kt
├── db/
│   ├── AppDatabase.kt
│   ├── HotelDao.kt
│   └── DatabaseProvider.kt
├── firebase/
│   ├── FirebaseAuthManager.kt
│   ├── CustomHotelRepository.kt
│   └── BookingRepository.kt
├── repositories/
│   └── HotelRepository.kt
├── services/
│   ├── HotelBookingFCMService.kt
│   └── NotificationListenerService.kt
├── utils/
│   ├── NotificationHelper.kt
│   └── ShakeDetector.kt
└── viewmodels/
    ├── AuthViewModel.kt
    ├── HotelListViewModel.kt
    ├── HotelDetailViewModel.kt
    └── LocationViewModel.kt
```

---

## Firestore Data Model

```
users/{uid}
  fullName, email, role, createdAt, fcmToken, points

hotels/{auto-id}
  firestoreId, ownerUserId, name, city, price, rating,
  imageUrl, description, latitude, longitude, isAvailable, createdAt

bookings/{auto-id}
  firestoreId, hotelId, hotelName, hotelCity, hotelImageUrl,
  checkIn, checkOut, pricePerNight, guestCount, guestNames,
  guestUserId, guestUserName, hostUserId, status, bookedAt

notifications/{auto-id}
  recipientUid, title, body, createdAt, read
```

### Required Firestore Indexes

The following composite indexes are needed (Firestore will print a direct link to create them on first query):

- `hotels`: `ownerUserId` ASC + `createdAt` DESC
- `bookings`: `guestUserId` (no orderBy — sorted in-memory)
- `bookings`: `hostUserId` (no orderBy — sorted in-memory)
- `notifications`: `recipientUid` + `read` + `createdAt`

---

## Room Database

Room is retained **only** for the `favorite_hotels` table (favourites are a personal device preference and do not need cross-device sync).

Current schema version: **10**

| Migration | Change |
|---|---|
| 1 → 2 | Added `imageUrl` to `favorite_hotels` |
| 2 → 3 | Added `bookings` table |
| 3 → 4 | Added `users` table |
| 4 → 5 | Added `hotelId` to favorites and bookings |
| 5 → 6 | Added `role` to users |
| 6 → 7 | Added `custom_hotels` table |
| 7 → 8 | Dropped `users` (moved to Firebase Auth + Firestore) |
| 8 → 9 | Dropped `bookings` (moved to Firestore) |
| 9 → 10 | Dropped `custom_hotels` (moved to Firestore) |

---

## Setup

### Prerequisites
- Android Studio Hedgehog or later
- JDK 21
- A Firebase project with **Authentication** (Email/Password) and **Firestore** enabled

### Steps

1. Clone the repository.
2. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com).
3. Enable **Email/Password** sign-in under Authentication → Sign-in method.
4. Enable **Cloud Firestore** and set your security rules.
5. Download `google-services.json` and place it in the `app/` directory (replacing the existing placeholder).
6. Open the project in Android Studio and let Gradle sync.
7. Run on a device or emulator (API 24+).

### Permissions

The app requests the following permissions at runtime where required:

- `INTERNET` — network access
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` — distance calculation and AR compass
- `CAMERA` — AR view passthrough
- `POST_NOTIFICATIONS` — local booking notifications (Android 13+)

---

## Hotel ID Scheme

Static hotels use integer IDs 1–999. Host-created (Firestore) hotels are assigned a synthetic integer ID of `1000 + index` to avoid collisions, while their actual Firestore document ID is stored in `Hotel.firestoreId`. This lets all existing Intent-based navigation continue to use `Int` hotel IDs without a breaking refactor.

---

## Localisation

The app supports **Bulgarian** (default) and **English**. String resources live in:

- `res/values/strings.xml` — Bulgarian
- `res/values-en/strings.xml` — English

The selected language is persisted in `SharedPreferences` and applied via `AppCompatDelegate.setApplicationLocales()`.

---

## License

This project is for educational purposes.
