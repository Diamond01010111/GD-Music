# GD Application

An Android online music player built with Kotlin, Java, and Jetpack Compose. The application uses the **GD Music Platform API** to search for music and retrieve playback URLs, artwork, and lyrics. It is currently being developed as a learning project for Android Compose, Media3, and network programming.

> This project is still under development and is intended only for personal study and technical discussion. Do not use it commercially or download, redistribute, or otherwise misuse copyrighted music.

[简体中文](README.md) | English

## Current Status

The original Java View interface is being migrated to Jetpack Compose while the existing Java networking and playback components are retained.

Features completed or currently being integrated:

- Edge-to-edge UI built with Jetpack Compose and Material 3
- Navigation between Home, Favorites, NetEase Playlists, and Search
- Song/artist search and album search
- Music source selection
- Recent search history
- Search result list with per-track actions
- Online retrieval of playback URLs and album artwork
- Audio playback with Media3 ExoPlayer
- Mini player
    - Album artwork, song title, and artist
    - Play and pause controls
    - Playback progress indicator
    - List repeat, repeat-one, and shuffle modes
    - Access to the current playback queue
- Playback queue
    - Fixed-height bottom sheet
    - Highlight for the currently playing track
    - Tap a track to start playback
    - Add a track to play next
    - Remove individual tracks
    - Clear the entire queue
- Local favorites support (still being improved)

Planned improvements:

- NetEase playlist search and playlist details
- Complete favorites and local playlist management
- Synchronized lyrics display
- Better playback errors, network error handling, and retry support
- Background playback, notification controls, and audio focus handling
- Seekable playback progress bar
- UI state persistence and process restoration
- UI polish, animations, and dark theme improvements
- Unit and UI tests

## Tech Stack

- Kotlin
- Java
- Jetpack Compose
- Material 3
- AndroidX Media3 / ExoPlayer
- OkHttp
- Gson
- Coil 3
- SharedPreferences for search history and local favorites

## Project Structure

The recommended structure at the current stage is shown below. It may continue to change during the Compose migration.

```text
app/src/main/java/com/diamond/gdapplication/
├── ComposeMainActivity.kt          # Compose entry point and player state bridge
├── GdMusicApi.java                 # GD Music Platform API client
├── MusicController.java            # Playback queue and mode controller
├── PlayerManager.java              # Media3 ExoPlayer wrapper
├── Track.java                      # Track data model
├── LocalPlaylistStore.java         # Local favorites storage
├── data/
│   └── SearchHistoryStore.kt       # Search history storage
├── model/
│   └── SearchModels.kt             # Page, search category, and source models
└── ui/
    ├── MusicApp.kt                 # Navigation and the main Scaffold
    ├── components/
    │   ├── MiniPlayer.kt
    │   └── QueueBottomSheet.kt
    └── screens/
        ├── HomeScreen.kt
        ├── SearchScreen.kt
        ├── SearchResultsScreen.kt
        ├── FavoriteScreen.kt
        └── NeteasePlaylistScreen.kt
```

## Requirements

- Android Studio, preferably the current stable release
- JDK 17
- Android SDK
- An Android Gradle Plugin version that supports Jetpack Compose
- A network connection that can access the GD Music Platform API

## Running the Project

1. Clone the repository and open it in Android Studio:

   ```bash
   git clone <repository-url>
   cd GD_Application
   ```

2. Wait for Gradle Sync to finish.

3. Ensure that `AndroidManifest.xml` contains the Internet permission:

   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

4. Select an Android emulator or physical device.

5. Click **Run** in Android Studio.

If the app icon does not appear after installation, make sure the app has been run successfully and that the launcher activity contains the `MAIN` and `LAUNCHER` intent filter.

## GD Music Platform API

This project uses the following third-party service:

- API endpoint: `https://music-api.gdstudio.xyz/api.php`
- Attribution: **GD Music Platform (music.gdstudio.xyz)**
- Documented rate limit: no more than 50 requests within five minutes
- Documented stable sources: `netease`, `joox`, and `bilibili`

Music sources listed by the application:

| Music source | Parameter | Recommendation |
| --- | --- | --- |
| NetEase Cloud Music | `netease` | Recommended |
| JOOX | `joox` | Recommended |
| Bilibili | `bilibili` | Recommended |
| Tencent Music | `tencent` | Unstable source |
| Kuwo | `kuwo` | Unstable source |
| Tidal | `tidal` | Unstable source |
| Qobuz | `qobuz` | Unstable source |
| Apple Music | `apple` | Unstable source |
| YouTube Music | `ytmusic` | Unstable source |
| Spotify | `spotify` | Unstable source |

Main API requests:

```text
# Search
GET /api.php?types=search&source={source}&name={keyword}&count={count}&pages={page}

# Retrieve a playback URL
GET /api.php?types=url&source={source}&id={trackId}&br={quality}

# Retrieve album artwork
GET /api.php?types=pic&source={source}&id={picId}&size={size}

# Retrieve lyrics
GET /api.php?types=lyric&source={source}&id={lyricId}
```

Album searches currently append `_album` to the source parameter—for example, `netease_album`.

### API Usage Notes

- Attribute the service as “GD Music Platform (music.gdstudio.xyz)” when using its API.
- Avoid concurrent or high-frequency requests. Client-side throttling and caching are recommended.
- Some sources may be temporarily unavailable, and the returned audio quality may be lower than requested.
- Playback URLs may expire and should not be stored permanently.
- The currently available API documentation does not provide a complete NetEase playlist search endpoint, so playlist support is still pending.

## Known Issues

- The project is still migrating from Java Views to Compose, so some legacy screens or entry points may remain.
- Sources that are not marked as recommended may be unavailable.
- Favorites are currently stored locally and may be lost when application data is cleared.
- The queue, current track, and playback position are not yet fully restored after the application process is terminated.
- Online content and service availability depend on third-party providers.

## Development Guidelines

- Keep Compose UI code in the `ui` package; do not place networking or playback logic directly inside composables.
- `MusicController` manages the playback queue, current index, and playback mode.
- `PlayerManager` handles interactions with the Media3 Player only.
- Player state is sent to `ComposeMainActivity` through listener callbacks and then passed to `MusicApp` as state.
- Call `notifyQueueChanged()` after modifying the queue and `onPlayModeChanged()` after changing the playback mode.
- Respect the API rate limit and provide clear user-facing messages when requests fail.

## Disclaimer

This is a non-commercial educational project. Music, artwork, lyrics, and other content are provided by third-party online services and remain the property of their respective authors and rights holders. This project does not host or distribute music files and makes no guarantee regarding the availability, legality, or content of third-party services.

If any content infringes your legal rights, please contact the relevant service provider or the project maintainer.

The GD Music Platform API is provided by GD Studio. Its documentation states that it is licensed under **CC BY-NC 4.0** and is intended for study purposes only.

## License

A license for this project has not yet been selected. Until a `LICENSE` file is added, do not use this project commercially or redistribute it without permission.