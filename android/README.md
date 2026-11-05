# J.A.R.V.I.S. Android

Local-first Android assistant starter built with Kotlin and Jetpack Compose.

## Included in this initial starter
- First-launch OpenRouter API-key setup.
- Encrypted local API-key storage using AndroidX Security Crypto.
- OpenRouter chat completion through the `openrouter/auto` model route.
- Local JSON conversation history.
- Dark HUD-inspired interface.

## Build
1. Install Android Studio with Android SDK 35 and JDK 17.
2. Open the `android/` folder in Android Studio.
3. Allow Gradle sync to complete.
4. Connect an Android 15 device or start an emulator.
5. Run the `app` configuration.

## Not implemented yet
This is the first runnable foundation, not the completed assistant. Wake-word detection, foreground microphone service, streaming speech interruption, British text-to-speech, cloud speech recognition, camera vision, Android automation, notification access, and full memory management still need separate implementations and device testing.

## API key safety
Never commit your OpenRouter key. The app asks for it at first launch and stores it locally using Android Keystore-backed encrypted preferences. A public APK can be reverse-engineered, so do not distribute a build containing your personal key. This starter calls OpenRouter directly from the device; internet access is required.

## Android restrictions
Always-on microphone behavior must use a user-visible foreground service and comply with Android microphone permissions and background-start restrictions. Continuous wake-word operation cannot be guaranteed under every device manufacturer's battery-management policy.
