# GitHub Streak Widget

Simple Android app and home-screen widget for showing a public GitHub activity streak for any username.

The app does not need a GitHub token, login, or backend server. It reads the public GitHub contribution calendar that anyone can see on a GitHub profile.

## Why this exists, and how it was built

I wanted a home-screen streak counter that did not need a token, a login, or a
server. I could not find one I liked, so I built the one I wanted.

**This app was built with heavy AI assistance** — vibe coded, to put it plainly.
I decided what it should do, tested it on my own phone, and shipped it. I did
not write most of this code by hand and I am not going to imply otherwise.

It is small, it works, and the APK is below. Read the source before trusting it
with anything that matters.

## Download

[Download latest APK](https://github.com/Creepyrishi/GitStreakWidget/releases/latest/download/github-streak-widget.apk)

[View all releases](https://github.com/Creepyrishi/GitStreakWidget/releases)

## Screenshots

<p>
  <img src="imgs/inapp_image.jpeg" alt="GitHub Streak app screen" width="280">
  <img src="imgs/home.jpeg" alt="GitHub Streak home-screen widget" width="280">
</p>

## Features

- Track any public GitHub username.
- Show the current active-day streak in an Android home-screen widget.
- No GitHub token required.
- No account login required.
- Manual refresh from the app or widget.
- Automatic refresh every 4 hours using WorkManager.
- Cute lightweight mascot states for active, warning, reset, and error states.
- Designed to be small and simple for lower-memory development machines.

## Streak Rule

A day counts as active when GitHub's public contribution graph shows at least one visible contribution for that day.

The app ignores the actual contribution count. It only checks whether the day was active or inactive.

```text
If today is active:
    count streak ending today
Else if yesterday was active:
    keep counting streak ending yesterday
Else:
    show 0
```

This means the streak resets to `0` when both today and yesterday are inactive.

## Data Source

The app fetches public contribution data from:

```text
https://github.com/users/{username}/contributions
```

Because this is tokenless, it follows GitHub's public contribution graph. GitHub may include visible commits, pull requests, issues, reviews, and other public contribution types.

## Privacy

- The app stores only the selected GitHub username and cached streak result on the device.
- No GitHub token is requested.
- No password is requested.
- No analytics or tracking backend is used.
- Network requests go directly from the device to GitHub's public contribution page.

## Limitations

- Private contribution details are not available without a GitHub token.
- The parser depends on GitHub's public contribution page structure.
- If GitHub changes that page, the parser may need an update.
- Android widgets do not support rich continuous animation efficiently, so the mascot uses lightweight state/frame changes.

## Build Without Android Studio

This project can be built with command-line tools only.

Required tools:

- JDK 17
- Android SDK command-line tools
- Android platform `35`
- Android build tools `35.0.0`
- Gradle `8.10.2` or compatible

Debug build:

```bash
gradle :app:assembleDebug --no-daemon --max-workers=2
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For GitHub Releases, upload the APK asset as:

```text
github-streak-widget.apk
```

Install on a connected phone:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Colab Build

You can build this project on Google Colab by uploading the project folder as a zip, then running this in a notebook cell from the project root:

```bash
apt-get update
apt-get install -y openjdk-17-jdk unzip curl
curl -L -o android-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
mkdir -p /content/android-sdk/cmdline-tools
unzip -q android-tools.zip -d /content/android-sdk/cmdline-tools
mv /content/android-sdk/cmdline-tools/cmdline-tools /content/android-sdk/cmdline-tools/latest
yes | /content/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/content/android-sdk "platform-tools" "platforms;android-35" "build-tools;35.0.0"
curl -L -o gradle.zip https://services.gradle.org/distributions/gradle-8.10.2-bin.zip
unzip -q gradle.zip -d /content/gradle
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/content/android-sdk
export ANDROID_SDK_ROOT=/content/android-sdk
/content/gradle/gradle-8.10.2/bin/gradle :app:assembleDebug --no-daemon --max-workers=2
```

Download the generated APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release Notes

Current version: `1.0`

- Initial Android app.
- Home-screen widget.
- Tokenless public GitHub contribution streak.
- 4-hour automatic refresh.
- Manual refresh.

## Tech Stack

- Kotlin
- Jetpack Compose
- Jetpack Glance
- WorkManager
- DataStore Preferences
- OkHttp
- Jsoup
