# Lake Town Turf 🏟️ (Android App)

A premium, feature-rich Android application built for residents and guests to seamlessly book and manage turf slots for Lake Town Society.

## 📱 Features

- **Authentication & Roles**: Google Sign-In via Firebase Auth. Accounts are securely gated by an Admin Approval process. Distinguishes between **Society Residents** (Flat No) and **Outsiders/Guests** (Full Address).
- **Slot Booking & Real-Time Sync**: Browse turf slots dynamically. Uses Firestore Snapshot Listeners to instantly update slot availability (e.g. if another user books a slot, it vanishes from your screen in milliseconds).
- **Secure Payments**: Integrated **Razorpay SDK** for processing slot fees and guest add-on fees securely. 
- **Waitlist System**: Full waitlist architecture. If a slot is booked, users can join the waitlist and receive an automated push notification if the slot is cancelled by the original owner.
- **Push Notifications**: Powered by Firebase Cloud Messaging (FCM) and Netlify backend functions to deliver instant transactional alerts:
  - Account Approved/Rejected
  - Booking Confirmed
  - Slot opened up (Waitlist)
- **Premium User Interface**: Built entirely with **Jetpack Compose** featuring a sleek, modern aesthetic, custom AMOLED Dark Mode/Light Mode theming, glassmorphism elements, and smooth micro-animations.
- **Cancellations & Refunds**: Users can cancel their own bookings, automatically triggering a Razorpay refund via the backend and updating the slot to "Available".

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin (100%)
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
- **Asynchronous Data:** Kotlin Coroutines & Flow (`StateFlow` / `MutableStateFlow`)
- **Dependency Injection / State Management:** ViewModels injected via Compose Navigation.
- **Database:** Firebase Firestore (NoSQL, Realtime)
- **Backend/Webhooks:** Netlify Serverless Functions (`laketownturf-admin`)
- **Payments:** Razorpay Checkout SDK
- **Image Loading:** Coil (Compose)
- **Navigation:** Jetpack Navigation Compose

## 📁 Key Components

- `ui/home/`: Contains the `HomeScreen` and `HomeViewModel` for date selection, slot browsing, waitlist joining, and Razorpay checkout triggers.
- `ui/bookings/`: Contains `BookingsScreen` to view Past/Upcoming bookings, generate PDF receipts, and cancel active bookings.
- `ui/profile/`: Handles dynamic rendering for Residents vs Guests and houses the Dark Mode toggle.
- `ui/auth/`: Google Sign-In flows, Pending Approval gate, and new profile creation.
- `data/repository/`: Abstracted data layer (`AuthRepository`, `BookingRepository`, `UserRepository`) connecting the UI to Firestore.
- `data/api/`: Network layer containing `ApiClient.kt` for triggering backend push notifications and refunds via Netlify.

## 🚀 Getting Started

1. Clone the repository.
2. Ensure you have the `google-services.json` file placed in the `app/` directory (required for Firebase to initialize).
3. Build and run via Android Studio (Target SDK 36, Min SDK 26).
4. For backend features (Refunds, Push Notifications) to work, ensure the corresponding `laketownturf-admin` React project is deployed on Netlify with valid Firebase Admin credentials.
