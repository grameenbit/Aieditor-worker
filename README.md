# Code AI - Mobile IDE

An AI-powered mobile code editor for Android with Gemini and OpenRouter support.

## Features
- AI Chat with Gemini 2.0/2.5 and OpenRouter models
- File Explorer with create/rename/delete/move
- Syntax highlighting (Kotlin, Java, XML)
- Terminal output panel
- Build Android projects directly from device
- Image and file attachments in chat
- Custom model support via OpenRouter

## Setup

### Prerequisites
- Android Studio or Gradle 8.5+
- JDK 17

### Building
```bash
chmod +x gradlew
./gradlew assembleDebug
```
APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions
Push to `main` or `master` branch to automatically build APKs.
Download from the **Actions** tab → select workflow run → **Artifacts**.

## Configuration
On first launch:
1. Go to **Settings** tab
2. Set your **Gemini API Key** (get from [Google AI Studio](https://aistudio.google.com))
3. Set your **Project Path** to open an existing project
4. Or tap **Create Project** to scaffold a new Android project

## OpenRouter Support
- Switch provider to **OpenRouter** in Settings
- Add custom models with any OpenRouter model ID
- Each custom model can have its own API key
