# VideoDownloader

A free, open-source Android app for downloading videos from links and saving them to your device.
Built with Kotlin, Jetpack Compose and Clean Architecture (domain / data / presentation).

## Features
- Paste or share a video link and pick a quality
- Background downloads with a foreground-service notification
- Download history, saved to the device's media store
- Status saver (via Storage Access Framework)

## Tech stack
Kotlin · Jetpack Compose (Material 3) · Hilt · Coroutines · Navigation Compose ·
[youtubedl-android](https://github.com/yausername/youtubedl-android) (yt-dlp + FFmpeg)

Min SDK 29 · Target SDK 36

## Build
1. Install Android Studio and JDK 11+.
2. Clone the repo and open it in Android Studio (it creates `local.properties` for your SDK path).
3. Run the `app` configuration, or `./gradlew assembleDebug`.

## Disclaimer
This software is provided for **personal use only**. You are responsible for complying with the
terms of service of any website you download from and with the copyright laws of your country.
Only download content you own or have permission to download. The author does not host or
distribute any copyrighted content and accepts no liability for misuse.

## License
Licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE).
This is required because the app depends on youtubedl-android, which is GPL-3.0.
Forks and derivative works must also be released under GPL-3.0 with source available.

## Third-party libraries and credits
| Library | License |
|---|---|
| [youtubedl-android](https://github.com/yausername/youtubedl-android) (`library`, `ffmpeg`) | GPL-3.0 |
| [yt-dlp](https://github.com/yt-dlp/yt-dlp) (bundled via youtubedl-android) | Unlicense |
| [FFmpeg](https://ffmpeg.org/) (bundled via youtubedl-android) | LGPL-2.1+ / GPL (build-dependent) |
| AndroidX (Core, Activity, Lifecycle, Navigation) and Jetpack Compose | Apache-2.0 |
| [Dagger Hilt](https://github.com/google/dagger) | Apache-2.0 |
| [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Apache-2.0 |
| JUnit 4 (tests only) | EPL-1.0 |

Thanks to the authors and maintainers of all of the above.
