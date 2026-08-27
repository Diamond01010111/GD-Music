# GD Application

An Android online music player built with Kotlin, Java, and Jetpack Compose. This project is intended for learning Android development, Media3 audio playback, Jetpack Compose UI development, and network programming.

> This project is intended for study and technical discussion only. Follow applicable laws, third-party terms of service, and copyright requirements. Do not use it to download, redistribute, or commercially exploit unauthorized music content.

[简体中文](README.md) | English

## Features

### Music Search and Playback

- Search by song, artist, or album
- Switch between multiple music sources
- Save recent search history
- Retrieve playback URLs and album artwork online
- Play audio with AndroidX Media3 / ExoPlayer
- Play, pause, and display playback progress
- List repeat, repeat-one, and shuffle modes
- Play next, switch queue tracks, remove individual tracks, and clear the queue

### Local Favorites

- Create multiple local favorite playlists
- Add songs from search and browsing screens to a selected favorite
- Browse favorite details and play all tracks
- Remove tracks from a favorite
- Delete favorite playlists
- Use the first track's artwork as the playlist cover
- Persist local data with SharedPreferences

### NetEase Cloud Music Playlists

- Enter a NetEase Cloud Music user ID
- Load the user's publicly visible created and subscribed playlists
- Display created and subscribed playlists in separate sections
- Show playlists in a two-column artwork grid
- Remember the user ID on the device
- Refresh automatically when the page opens
- Refresh manually or sign out of the current user

The application currently displays public playlist metadata only. Viewing playlist tracks and playing NetEase playlists are not yet supported.

### Android Auto

- Provides Favorites and NetEase Playlists as the two top-level car destinations
- Browses local favorite playlists and their tracks
- Plays favorite tracks from Android Auto
- Shows an import-on-phone notice under NetEase Playlists

## Tech Stack

- Kotlin
- Java
- Jetpack Compose
- Material 3
- AndroidX Media3 / ExoPlayer
- OkHttp
- Coil 3
- SharedPreferences

## Requirements

- Android Studio
- JDK 17
- Android SDK
- Minimum Android version: Android 6.0 (API 23)
- Target Android version: API 36
- Network access to the required third-party music services

## Running the Project

1. Clone the repository:

   ```bash
   git clone https://github.com/Diamond01010111/GD_Application.git
   cd GD_Application
   ```

2. Open the project in Android Studio.
3. Wait for Gradle Sync to finish.
4. Connect an Android device or start an emulator.
5. Click **Run** to build and install the application.

To test Android Auto, use the Desktop Head Unit supplied with Android Studio or connect a compatible Android Auto device and vehicle.

## Third-Party Services

The project uses the GD Music Platform API to search for music and retrieve playback URLs, artwork, and lyric data:

- API: `https://music-api.gdstudio.xyz/api.php`
- Attribution: GD Music Platform (music.gdstudio.xyz)
- Documented rate limit: no more than 50 requests within five minutes
- Documented stable sources: `netease`, `joox`, and `bilibili`

The NetEase playlist screen uses a public NetEase Cloud Music endpoint to retrieve publicly visible playlists for a specified user. The endpoint, response format, and availability may change without notice. Private playlists are not displayed.

You are responsible for reviewing and following each third-party service's terms, licenses, and rate limits.

## Planned Improvements

- NetEase playlist details and track import
- Lyrics display and synchronized scrolling
- Seekable playback progress
- More complete background playback and notification controls
- Network error messages, retries, and caching
- Playback and screen state restoration
- Dark theme, animations, and UI polish
- Unit and UI tests

## Known Limitations

- Third-party music sources may become temporarily unavailable
- Returned audio quality may be lower than requested
- Playback URLs may expire
- Clearing application data removes local favorites and the saved NetEase user ID
- The queue, current track, and playback position are not fully restored after process termination
- NetEase playlist browsing and playback are not yet implemented in Android Auto

## Disclaimer

This project does not host, provide, or distribute music files. Music, artwork, lyrics, playlist information, and other third-party content belong to their respective authors, platforms, or rights holders.

The project author makes no guarantee regarding the availability, accuracy, stability, or legality of third-party services or content. Users are responsible for their use of those services.

The GD Music Platform API is provided by GD Studio. Its documentation states that it is licensed under **CC BY-NC 4.0** and intended for study purposes only. That license applies to the relevant third-party service and does not mean that this project's author owns or can relicense its music or other content.

## License

Code written for this project is available under the [MIT License](https://opensource.org/license/mit). The MIT License does not cover third-party APIs, dependencies, music, artwork, lyrics, trademarks, or other third-party content; each remains subject to its own terms and license.
