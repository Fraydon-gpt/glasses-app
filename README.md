# My Glasses

Rob's easy-read Android app for the MemoMind One glasses.

- **Shopping list** and **To-do list**: add items by typing or speaking, show them on the lens.
- **Memory Keeper**: "my spare keys are in the blue bowl", then ask "keys" later.
- **Timers**: quick timers and reminders that pop up on the lens and on the phone, even when the app is closed.
- **Maps**: big shortcut into Google Maps with your recent places.

## Getting the app

Every change pushed to `main` is built by GitHub automatically.
The newest APK is always on the **Releases** page, under **latest**, as `MyGlasses.apk`.

## How it talks to the glasses

Classic Bluetooth (RFCOMM, serial port UUID `00001101-…`, falling back to `00007033-…`),
using the GM packet format from MemoMind's public developer guide
(github.com/memomind-open/plugin-open-platform). The screens are in
`app/src/main/assets/index.html`; the phone side is in `app/src/main/java/com/rob/glasses/`.
