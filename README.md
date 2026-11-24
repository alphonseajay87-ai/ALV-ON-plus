# ALVéON+

ALVéON+ is a free, local-first Android TV media player foundation built with Kotlin, Jetpack Compose for TV, Media3, Coil, Retrofit, and TheMovieDB metadata lookup.

## Build

1. Install Android Studio (or Android SDK command-line tools) and JDK 17.
2. Open this repository as a Gradle project and allow dependency sync.
3. Build the debug APK with `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).
4. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

The app targets API 34, supports API 21+, and advertises the Leanback launcher. Select a storage folder through the system folder picker; Android's Storage Access Framework grants access without broad filesystem access. Add HTTP(S) directory URLs or direct media URLs from the home screen. Media3's built-in TV controller exposes playback and available audio/subtitle tracks. Movie metadata lookup needs internet; playback and local browsing work offline.

## Logo

`app/src/main/res/drawable/logo.xml` is a simple vector placeholder used by the splash. Replace it with the supplied brand artwork (update the resource reference if you name it `logo.png`).

## Notes

The supplied TMDB API key is configured in `TmdbRepository.kt`. Treat client-distributed API keys as public; replace/restrict it for a production release if needed. Server support currently covers HTTP(S) directory listings and direct video URLs; SMB and WebDAV are not included in this initial compiling foundation.
