# ALVéON+

A free, offline-first media player for Android TV. Netflix / Apple TV style UI, pure black, silver accents.
No accounts, no ads, no paywalls.

**Stack:** Kotlin · Jetpack Compose for TV (`androidx.tv`) · Media3 / ExoPlayer · Coil · Retrofit + OkHttp · Material3
**SDK:** min 21 · target 34 · compile 35

## Features

- **Splash:** black screen, soft glow, the logo eases in, "ALVéON+" rises under it while its letters tighten, then a light sweep runs across (about 1.9 s).
- **First launch:** one short intro explaining the app.
- **Home (Apple TV style):** a full-screen backdrop that follows whatever you focus, a glass info card (title, year, rating, description, Play / My List), and rows of posters. Tabs: For You, Movies, Series, plus Search and Settings. The animated ALVéON+ logo sits top right.
- **Series:** episodes are grouped under one poster. Open it for the episode list, resume, and episode names from TMDB.
- **Player:** frosted glass control panel (seek bar, previous / back 10 s / play / forward 10 s / next, audio, speed), top bar with title and file facts (resolution, Dolby Vision / HDR, audio format) and the ALVéON+ logo, subtitle picker. **Pause shows only the story:** big title, episode name, description. Press OK to continue.
- **External subtitles:** `.srt`, `.vtt`, `.ass` / `.ssa` files placed next to a video (same base name) are automatically detected and appear in the Subtitles panel alongside embedded tracks. Works for local/USB (SAF) and `file://` paths.
- **Smart audio:** On every file the player picks the best audio track available — Dolby Atmos / TrueHD / 7.1 over 5.1 over stereo — unless you set a preferred language in Settings.
- **Preferred languages:** Settings → Preferred audio / subtitle language. Cycles through common languages; empty = best quality (audio) or Off (subs).
- **Recently Added:** Home and Movies tabs show a row sorted by file last-modified (USB / SAF folders).
- **Skip Intro & Next episode:** Skip Intro appears early in long titles; Next advances the series queue and marks the current episode finished.
- **Files:** MKV details (4K, IMAX-wide, Dolby Vision / HDR10 / HLG, Atmos, 7.1 / 5.1, audio and subtitle tracks) are read from the file itself.
- **Servers are set up in Settings:** web folder / direct link or WebDAV, optional login, "Test connection". Their movies appear on Home like local ones.
- **USB / internal storage:** pick a folder once with the system picker; no storage permission needed.
- **Movie info:** TheMovieDB (title, description, year, rating, one poster, one backdrop) matched from the file name, cached for offline use, can be switched off.
- **My List**, Continue Watching with resume, search, "Open with ALVéON+" from file managers.

## Build the APK with GitHub (no Android Studio needed)

1. Push this whole folder to a GitHub repository (the `.github/workflows` folder must be included).
2. Open the **Actions** tab → **Build Android TV APK** → **Run workflow**.
   (It also runs on every push to `main`.)
3. When it finishes, open the run and download the **ALVEON-plus-apk** artifact (a zip containing `ALVEON-plus.apk`).
4. Optional: push a tag like `v2.0.0` and the APK is attached to a GitHub Release, which gives a direct download link
   you can type into the "Downloader" app on the TV.

## Build locally

Needs JDK 17 and the Android SDK (API 35).

```
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Install on the TV

Enable Developer options → USB/network debugging on the TV, then `adb connect <tv-ip>` and
`adb install -r app-debug.apk`. Or copy the APK to a USB stick and open it with a file manager.

## Replace the logo

Overwrite these two files, keeping the names:

- `app/src/main/res/drawable/logo.png`: square PNG with transparent background (512×512 or more). Used on the splash, the home screen and as the app icon.
- `app/src/main/res/drawable-xhdpi/banner.png`: 640×360 PNG, the tile shown on the Android TV launcher.

## Project layout

```
app/src/main/java/com/alveon/plus/
  MainActivity.kt, AlveonApplication.kt, AppGraph.kt
  data/        LocalStore, TmdbApi/TmdbRepository, FileNameCleaner, ExternalSubtitles,
               StorageBrowser (SAF), ServerBrowser (HTTP + WebDAV), ServerAuthInterceptor
  navigation/  Routes, AlveonNavHost
  ui/          theme, components, splash, welcome, home, series, search, browser, server, settings, player
```

## Known limits

- **SMB shares are not supported** (Android has no built-in SMB client; it needs a third-party library). Use a NAS's WebDAV or web-folder option.
- Audio codecs like **DTS / Dolby TrueHD** depend on the TV's hardware decoders. If a file plays with no sound, try another audio track.
- MKV chapter names are not shown (the player library does not expose them), and the file's internal title tag is not read, so names come from the file name + TMDB.
- The TMDB key is in `TmdbApi.kt`. Keys shipped inside an app are visible to anyone; swap in your own if you publish widely.
- Server passwords are stored in the app's private storage (backups are disabled), not encrypted.
- The CI builds a **debug** APK, which is fine for sideloading. A signed release build would need a keystore.
- External subtitles work for local / SAF content. Network (HTTP/WebDAV) external subs are not auto-discovered yet (the server listing only returns video files).
