# Lake Town Turf 🏟️

A premium, feature-rich platform built for residents and guests to seamlessly book and manage turf slots for Lake Town Society, comprising a mobile application for users and a robust admin dashboard for management.

---

## 📱 Mobile App (Android)

The mobile application is designed to provide a seamless, secure, and user-friendly experience for booking turf slots.

### Features & Workflow

- **Authentication & Onboarding**: 
  - One-tap **Google Sign-In**.
  - **Profile Creation**: Users select their role. 
    - *Society Members* provide their Mobile Number, Block, and Flat Number.
    - *Guests/Outsiders* provide their Mobile Number and Full Address.
  - Users must accept the Terms of Service and Privacy Policy to proceed.
  
- **Admin Verification Gate**: 
  - After profile creation, the account goes into a pending state for Admin verification. 
  - The admin verifies the provided details (like Flat No. for residents). 
  - Once approved, the profile becomes active, unlocking the main app.

- **Booking Flow (Home Screen)**: 
  - Users can view day-wise slot availability. Only unbooked slots are open for booking.
  - During booking, users must enter details for each player (Name, Block, Flat No.).
  - **Guest Add-ons**: If guests are joining, their names are entered, and a ₹100 fee per guest is automatically added to the turf base price.
  
- **Payments (Razorpay)**: 
  - Integrated with the **Razorpay Payment Gateway** for secure online transactions.
  - **Success**: The slot is instantly booked, and a digital receipt is generated (eliminating paperwork).
  - **Failure/Rejection**: No slot is booked, and the user can try again safely.

- **Dynamic Cancellations & Refunds**: 
  - Users can cancel their upcoming bookings directly from the app (subject to cancellation policies and countdowns).
  - Refunds are processed dynamically back to the original payment source.

- **Waitlist & Slot Availability System**: 
  - If a desired slot is already booked, users can opt-in to be notified.
  - If the original user or an admin cancels that slot, it immediately becomes free.
  - An automated push notification is instantly sent to all users who opted into that slot's waitlist, giving them a chance to book it.

- **Bookings Management**: 
  - **Bookings Tab** categorizes history into Upcoming and Previous bookings.
  - Features a live countdown for cancellation eligibility.
  - Users can download digital PDF receipts for their records.

- **Profile & Settings**: 
  - Edit personal details like Name.
  - Toggle App Appearance (Dark/Light Mode).
  - View Maintenance Status (if the admin has paused bookings).
  - Access Privacy Policy and Terms of Service.
  - Secure Logout.

### 🔔 Notifications System
Notifications are crucial for keeping the user informed without them needing to constantly check the app:
- **Account Approved/Rejected**: Informs the user the moment their profile is reviewed by the admin.
- **Booking Confirmed**: Transactional assurance that their payment succeeded and the slot is theirs.
- **Waitlist Alerts**: Vital for maximizing turf utilization. Alerts interested users the second a cancelled slot becomes available again.
- **System Alerts**: Used for maintenance or manual admin announcements.

---

## 💻 Admin Dashboard (Web)

The admin web portal provides comprehensive control and analytics over the entire turf booking ecosystem.

### Features & Workflow

- **Dashboard**:
  - High-level metrics at a glance: Today's Revenue, Today's Bookings, Total Users, and Pending Approvals.
  - **Revenue Chart**: Visual analytics showing daily revenue trends over the last 7 days.

- **Users Management**:
  - View all registered users (Joined Date, Profile Details, Status).
  - **Universal Search Bar**: Conveniently search for users by any metric (Name, Email, Flat No, Phone).
  - Approve or Reject pending profile verifications directly from the list.

- **Slots Management**:
  - A day-wise calendar view showing all slots and their real-time statuses (Available, Booked, Passed).
  - Admins can manually block or cancel slots if necessary.

- **Bookings Ledger**:
  - Comprehensive view of all transactions.
  - **Filters**: Sort by All, Pending Verification, Upcoming, or Past bookings.
  - **Deep Search**: Search through bookings using literally anything—Razorpay Order ID, Payment ID, User Name, Flat No, etc.
  - **CSV Export**: Export the entire bookings ledger into a CSV file for accounting and auditing purposes.

- **Settings & System Controls**:
  - **Maintenance Mode**: Admins can temporarily pause all slot bookings. 
  - Can be configured with a custom Start and End time, or toggled manually until turned off. During this time, the mobile app reflects the maintenance status and blocks new checkouts.
