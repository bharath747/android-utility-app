# Android Utility App

A collection of small, practical Android utilities. The first utility is **Hotspot Auto-Off**.

## Hotspot Auto-Off

Configure a recurring local-time schedule:

- Every day
- Weekdays
- Weekends
- Any selected days
- Enable/disable the schedule
- Automatic rescheduling after reboot, time changes, and timezone changes
- Precise-alarm access is requested when available
- Scheduled notification with a shortcut to wireless settings

### Android hotspot limitation

Modern Android does not expose a general public API for an ordinary third-party app to switch off a hotspot that was created through the system Settings UI. Android 36 introduced `TetheringManager`, but its tethering-request model does not turn an existing user-created hotspot into a generally controllable resource for ordinary apps.

Therefore this first release deliberately does **not** use hidden APIs or reflection. At the scheduled time it posts a notification explaining the limitation and gives the user a shortcut to wireless settings.

This architecture leaves room for a future device-specific or privileged implementation without changing the scheduling model.

## Build locally

The project uses Android Gradle Plugin 8.13.2, Gradle 8.13, JDK 17, Kotlin 2.2.21 and compile/target SDK 36.

```bash
gradle assembleDebug
```

The APK is produced at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

Every push and pull request to `main` builds the debug APK. The APK is uploaded as the `android-utility-app-debug` workflow artifact.

You can also run the workflow manually using **Actions → Build Android APK → Run workflow**.
