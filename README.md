<p align="center">
  <img src="docs/banner.svg" alt="Spotipeng — A native Android music-player project." width="100%" />
</p>

<p align="center">
  <strong>Java · Android · Retrofit · MediaPlayer</strong><br />
  A Spotify-inspired music-player project with a searchable library, lyrics, and background playback.
</p>

<p align="center">
  <a href="#features">Features</a> · <a href="#getting-started">Getting started</a> · <a href="#backend-connection">Backend connection</a> · <a href="#project-structure">Project structure</a>
</p>

## Overview

Spotipeng explores a complete native Android listening interface: sign in, browse songs, open the player, follow lyrics, and keep playback controls available through a foreground service notification.

## Features

- **Song library:** search the catalog, switch between list and grid views, and sort by title or artist in either direction.
- **Playback controls:** play, pause, resume, seek, skip, shuffle, and loop.
- **Player interface:** mini-player, song details, artwork, and lyrics views.
- **Background playback:** `MusicService` coordinates Android’s `MediaPlayer` and notification controls.
- **Account flow:** login, registration, user information, and logout through the project’s API.

## Getting started

The repository uses **Android Gradle Plugin 8.1.0**, the **Gradle 8.0 wrapper**, **compile/target SDK 33**, and **minimum SDK 26** (Android 8.0). Use [JDK 17 for AGP 8.1](https://developer.android.com/build/releases/agp-8-1-0-release-notes#compatibility), Android SDK 33, and a compatible Android Studio installation.

```bash
git clone https://github.com/rizqiramadhannn/Spotipeng.git
cd Spotipeng
```

1. Open the project in Android Studio and let Gradle sync.
2. Configure the backend and media URLs described below.
3. Run the `app` configuration on an emulator or device with API 26 or later.

To produce a debug APK from the terminal:

```bash
./gradlew assembleDebug
```

On Windows, use `gradlew.bat assembleDebug`. The output is `app/build/outputs/apk/debug/app-debug.apk`.

## Backend connection

Update [`Constants.java`](app/src/main/java/com/example/spotipeng/utils/Constants.java):

| Setting | Purpose |
| --- | --- |
| `BASE_URL` | Base URL for the compatible Spotipeng API; retain the trailing `/` |
| `Song_URL` | Prefix used to construct the audio and lyrics URLs returned by the API |

The checked-in `BASE_URL` is a temporary ngrok address and needs to be replaced with your running backend. The app expects routes including:

```text
GET  spotipeng/api/v1/song
GET  spotipeng/api/v1/users
POST spotipeng/api/v1/users/login
POST spotipeng/api/v1/register
```

The backend and hosted audio files are separate dependencies. This repository does not provide an offline song catalog or access to Spotify’s service.

## Project structure

```text
app/src/main/java/com/example/spotipeng/
├── activity/     Login, registration, library, player, and lyrics screens
├── api/          Retrofit API definitions and authentication interceptor
├── events/       EventBus messages shared by playback components
├── fragment/     Mini-player and lyrics panels
├── model/        Song, user, and authentication models
├── service/      Background music playback
└── utils/        API and media URL configuration
```

---

A music-player experiment by [Rizqi Ramadhan](https://rizqiramadhan.dev).
