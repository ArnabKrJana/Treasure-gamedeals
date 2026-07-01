<p align="center">
  <img src="assets/banner.png" alt="Treasure Banner" width="100%">
</p>

<h1 align="center">🏴‍☠️ Treasure</h1>

<p align="center">
  <strong>The Ultimate Game Deal Tracker & Wishlist Companion</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack_Compose-1.5+-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose">
  <img src="https://img.shields.io/badge/Spring_Boot-3.2+-6DB33F?logo=springboot&logoColor=white" alt="Spring">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
</p>

---

## ✨ Overview

**Treasure** is a modern, high-performance game deal tracker designed for gamers who never want to miss a sale. By aggregating data from Steam, GOG, Epic Games, and more, Treasure provides a unified interface to discover "Hot Deals", "Historic Lows", and platform-specific bargains.

Built with **Jetpack Compose** on the front-end and a **Spring Boot BFF** on the back-end, it offers a seamless, offline-first experience with cloud synchronization via Google Drive.

---

## 📱 Features

- 🎯 **Deal Discovery**: Real-time tracking of platform-specific sales (Mac/Linux) and "Lowest Price Ever" alerts.
- 📺 **Immersive Media**: High-quality screenshots and trailers powered by **Media3 ExoPlayer**.
- 🔔 **Smart Alerts**: Customizable price-drop notifications so you buy only when the price is right.
- ☁️ **Cloud Sync**: Securely backup your wishlist using **Google Drive integration**.
- 🎨 **Material You**: Full **Material 3** implementation with **Dynamic Color** support.
- 🚀 **Performance**: Offline-first architecture with **Room DB** and **Paging 3**.

---

## 🛠 Tech Stack

### Frontend (Android)
<p align="left">
  <img src="assets/icons/kotlin.png" height="35" alt="Kotlin"> &nbsp;
  <img src="https://developer.android.com/static/images/logos/android.svg" height="35" alt="Android"> &nbsp;
  <img src="assets/icons/google.svg" height="35" alt="Google">
</p>

- **UI**: Jetpack Compose, Material 3, Motion Layout.
- **Architecture**: MVVM + Clean Architecture.
- **Data**: Room DB, DataStore, Paging 3.
- **Networking**: Retrofit 3.0, OkHttp 5.1, GSON.
- **Security**: Google Credential Manager, Security-Crypto.

### Backend (BFF)
<p align="left">
  <img src="assets/icons/spring.png" height="35" alt="Spring Boot"> &nbsp;
  <img src="assets/icons/postgres.png" height="35" alt="PostgreSQL"> &nbsp;
  <img src="assets/icons/redis.png" height="35" alt="Redis"> &nbsp;
  <img src="assets/icons/rabbitmq.svg" height="35" alt="RabbitMQ"> &nbsp;
  <img src="assets/icons/docker.svg" height="35" alt="Docker"> &nbsp;
  <img src="assets/icons/aws.svg" height="35" alt="AWS">
</p>

- **Core**: Spring Boot 3.2, Spring Security (JWT).
- **Data**: Spring Data JPA, Hibernate, PostgreSQL.
- **Caching & Messaging**: Redis, RabbitMQ.
- **DevOps**: Docker, AWS Elastic Beanstalk, Swagger/OpenAPI.

---

## 📸 Screenshots

<p align="center">
  <img src="assets/screenshots/screenshot_1.png" width="30%" alt="Home Screen">
  <img src="assets/screenshots/screenshot_2.png" width="30%" alt="Game Details">
  <img src="assets/screenshots/screenshot_3.png" width="30%" alt="Settings Screen">
</p>

---

## 🏗 System Architecture

```mermaid
graph LR
    A[Android App] --> B[Spring Boot BFF]
    B --> C[(PostgreSQL)]
    B --> D[(Redis)]
    B --> E[RabbitMQ]
    B --> F[External APIs: ITAD/Steam/IGDB]
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio Ladybug** (or newer)
- **JDK 17**
- A Google Cloud Project for Sign-In & Drive APIs.

### Installation
1. **Clone the repo**:
   ```bash
   git clone https://github.com/Arnab-Kumar-Jana/Treasure.git
   ```
2. **Setup Keys**: Create a `local.properties` file with your API keys.
3. **Build**: Sync Gradle and run the `:app` module.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

<p align="center">
  Developed with ❤️ by Arnab Kumar Jana
</p>
