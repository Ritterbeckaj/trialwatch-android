# Trialwatch (Android)

A real home-screen widget. Six buttons — **1d / 3d / 7d / 14d / 1m / 3m** — one tap logs a
free trial and arms three `AlarmManager` wakeups before the card is charged.

No server, no account, no network permission. Everything is on-device.

---

## Why this is native and the web version isn't

| | PWA (browser) | This app |
|---|---|---|
| Home-screen widget | icon only | real `AppWidget` you place on the launcher |
| Wakes a sleeping phone | only with push | `setAlarmClock()` — survives Doze, rings through DND |
| Survives reboot | no | `BOOT_COMPLETED` receiver re-arms everything |
| Opens the cancel page | tab | notification action → `ACTION_VIEW` |

`setAlarmClock()` is the strongest wakeup Android exposes to a normal app. It is what
alarm-clock apps use: the system treats it as a user-visible alarm, shows it in the status
bar, keeps it out of Doze batching, and lets it ring past Do Not Disturb.

---

## Build

```bash
cd trialwatch-android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in Android Studio (Iguana+) and hit Run.

Requirements: JDK 17+, Android SDK with `platforms;android-35` and `build-tools;35.0.0`.

First run on Android 13+: allow notifications. On Android 12+: allow exact alarms
(the app shows a banner until you do).

---

## Put the widget on the home screen

Long-press the launcher → **Widgets** → **Trialwatch** → drag the 4×2 widget out.

Tap `7` the moment you sign up for a 7-day trial. A translucent sheet opens with the
keyboard up: type the service name, tap **Arm alarm**. Done — about four seconds.

---

## What gets scheduled per trial

| when | what happens |
|---|---|
| T−3 days | notification, normal tone |
| T−1 day | notification, alarm tone |
| T−2 hours | `setAlarmClock` — full-screen alert + alarm sound |
| the notification itself | two actions: **Open cancel page** and **I cancelled it** |

Tapping **Open cancel page** leaves the app for the vendor's cancel URL, not its homepage.
Tapping **I cancelled it** marks the trial done, cancels the remaining alarms, and refreshes
the widget.

---

## Project map

```
app/src/main/java/io/trialwatch/
├── TrialWatchApp.kt          Application — channel + re-arm on cold start
├── data/
│   ├── Trial.kt              the record
│   ├── Durations.kt          the six buttons, shared by widget and app
│   ├── TrialStore.kt         SharedPreferences + org.json, no DB, no deps
│   └── CancelUrls.kt         service name → cancel URL, fuzzy matched
├── alarm/
│   ├── AlarmScheduler.kt     the three wakeups + widget refresh
│   ├── AlarmReceiver.kt      fires the notification
│   ├── DoneReceiver.kt       "I cancelled it" / snooze
│   ├── BootReceiver.kt       reboot + timezone change re-arm
│   └── Notify.kt             high-importance channel, full-screen intent
├── widget/
│   ├── TrialWidget.kt        Glance UI — the buttons
│   └── TrialWidgetReceiver.kt
└── ui/
    ├── MainActivity.kt       list, counts, exact-alarm banner
    ├── QuickAddActivity.kt   translucent capture sheet
    └── Theme.kt
```

To support a new service, add one line to `CancelUrls.table`. Unknown names fall back to a
`cancel <name> subscription` search, so the button is never dead.

---

## Known limits

- Alarms are exact only while `canScheduleExactAlarms()` is true. If the user revokes it,
  the schedule degrades to a 10-minute window instead of silently never firing — deliberately
  visible in code, see `AlarmScheduler.schedule`.
- Full-screen intent is granted automatically for alarm-category apps only in recent
  Android versions; if the system refuses, the notification still posts with sound.
- Store-billed subscriptions (App Store / Play) must be cancelled in the store's own
  subscription manager. The Apple and Google rows in `CancelUrls` point there on purpose.
