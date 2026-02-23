# 💊 R-Tweaks: Advanced Dynamic Capsule for Android Handhelds

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)

**R-Tweaks** is a lightweight, high-performance background service designed for Android handheld consoles. It introduces an interactive "Dynamic Capsule" that provides real-time system feedback without interrupting your gameplay. 

---

## ✨ Key Features

### 🌡️ Intelligent Thermal Monitoring
* **Real-time Detection**: Constantly monitors the battery and SoC temperature.
* **Critical Alerts**: Automatically triggers a high-visibility warning if the device exceeds **45°C**, preventing thermal throttling during heavy emulation (PS2, Switch, GameCube).

### 🎵 Multimedia Integration
* **Spotify Metadata Sync**: Seamlessly displays current track titles and artist names using broadcast receivers.
* **Low-Impact UI**: The capsule appears only during track changes and disappears automatically to keep the screen clear.

### ⚡ Power & Battery Management
* **Charging States**: Visual confirmation when a power source is connected or disconnected.
* **Low Battery Protocol**: A dedicated "Critical Battery" alert at 15% with a unique icon to ensure you save your game state in time.

### 🚀 Built for Persistence
* **Auto-Boot Execution**: Using `RECEIVE_BOOT_COMPLETED`, the service starts automatically when the console is powered on.
* **Persistent Foreground Service**: Engineered to remain active even during high RAM usage, ensuring your monitors never fail while gaming.

---

## 🔧 Installation & Pro Tips

### 1. Download & Install
Download the latest `R-Tweaks.apk` from the **[Releases](https://github.com/Benyiiiii/R-Tweaks_NotificationApp/releases/tag/download)** section and install it on your Android device.

### 2. Grant Permissions
The app requires:
* **Display over other apps**: To render the capsule on top of games.
* **Notification Access**: For media sync and background service stability.
