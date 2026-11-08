# Wear OS AI Hub (`ai_wearos`)

A dedicated Wear OS native application bringing **ChatGPT**, **Google Gemini**, and **Perplexity** directly to your smartwatch (optimized for Samsung Galaxy Watch 4/5/6/7/Ultra, Google Pixel Watch 1/2/3, and all Wear OS round displays).

---

## 🎯 What We Built & Core Features

- **⚡ 1-Tap AI Model Switching**: Direct, instantaneous access to your favorite AI assistants:
  - 🟢 **ChatGPT** (`chatgpt.com`)
  - 🔵 **Google Gemini** (`gemini.google.com`)
  - 🌐 **Perplexity** (`perplexity.ai`)
- **🎙️ Direct Voice Dictation (Speech-to-Text)**: Dedicated microphone button to speak your prompts naturally on your wrist without fiddling with tiny on-screen keyboards.
- **🔍 True Desktop-Grade Viewport Zoom**: Runs at 1:1 pixel density with built-in viewport extension. Rotating the watch crown smoothly zooms between 50% and 200% with true responsive text re-flow and active touch scrolling.
- **🔒 Persistent Login & Sessions**: Integrated secure web engine preserves cookies, Google / OpenAI account sessions, passwords, and biometric credentials across app restarts.
- **⬛ True AMOLED Dark Mode**: 100% pure black backgrounds (`#000000`) designed for maximum battery efficiency on OLED watch screens.
- **⌚ Rotary Crown Controls**: Clockwise rotates in, counter-clockwise zooms out. Last zoom level is remembered per model.

---

## 🚀 How to Build & Install

### 1. Build via Gradle
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### 2. Install via ADB Wi-Fi
```bash
adb connect <WATCH_IP>:<PORT>
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📜 Credits & Architecture

- Native Wear OS project built with Android SDK, GeckoView web engine, and One UI Watch ergonomics.
- Developed by **Gustavo Lara** ([@gusdslara](https://github.com/gusdslara)).
