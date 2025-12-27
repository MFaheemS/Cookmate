A full-stack social media Android application built with Kotlin and Firebase, featuring real-time messaging, audio/video calling, stories, and push notifications.
The app focuses on real-time communication, modern social features, and production-level Android components.

 Features
 User Management

User authentication using Firebase Authentication

User profiles with profile pictures and basic information

 Posts & Feed

Create and view posts in a real-time feed

Optimized UI using RecyclerView

 Real-Time Chat

One-to-one real-time messaging using Firebase Realtime Database

Media (image) sharing in chat

Message edit and delete within a limited time window

Online/offline presence indicators

Screenshot detection alerts for chat privacy

 Audio & Video Calling

Real-time audio and video calls using Agora RTC SDK

Incoming call handling via foreground services

Dynamic channel creation and lifecycle management

Stories (Instagram-style)

Stories with 24-hour auto-expiration

Animated progress bars

Swipe gestures for navigation

Per-user story view tracking

 Notifications

Push notifications using Firebase Cloud Messaging (FCM):

Follow requests

New messages

Incoming call alerts

 Tech Stack

Language: Kotlin

Platform: Android

Backend / Services:

Firebase Authentication

Firebase Realtime Database

Firebase Cloud Messaging (FCM)

Real-Time Communication: Agora RTC SDK

Android Components:

RecyclerView

ViewPager2

Foreground Services

ContentObserver

Architecture: REST-based interactions & real-time listeners

 Notable Implementations

Screenshot Detection System
Implemented using ContentObserver and MediaStore monitoring to detect screenshots and notify chat participants automatically.

Reliable Call Handling
Used foreground services to ensure incoming audio/video calls are handled even when the app is in the background.

Real-Time Presence Tracking
Online/offline indicators implemented using Firebase real-time listeners.

 Project Structure (High-Level)
app/
├── activities/
├── adapters/
├── fragments/
├── models/
├── services/        # Foreground services (calls)
├── utils/
└── firebase/

 How to Run

Clone the repository

git clone https://github.com/your-username/social-media-android-app.git


Open the project in Android Studio

Add your Firebase configuration:

Create a Firebase project

Enable Authentication, Realtime Database, and FCM

Download google-services.json and place it in the app/ directory

Add Agora credentials:

Create an Agora project

Add your App ID in the appropriate config file

Build and run on an emulator or physical device
