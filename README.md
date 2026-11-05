# SAJI GRAND CITY 3D

An original, lightweight Android 3D city-exploration prototype. This is an early playable foundation, not a finished open-world game.

## Current prototype
- Landscape Android game screen using OpenGL ES 2.0.
- Third-person follow camera.
- Procedurally drawn city blocks, roads, sidewalks, trees, street lights and moving traffic.
- Simple player character and on-screen joystick, GO and STOP controls.
- No external game assets or paid SDKs are required for this prototype.

## Build an APK
Every push to `main` starts the GitHub Actions workflow **Build SAJI Grand City APK**.

1. Open the repository on GitHub.
2. Open the **Actions** tab.
3. Select **Build SAJI Grand City APK**.
4. Open the latest run after it finishes successfully.
5. Under **Artifacts**, download `SAJI-GRAND-CITY-3D-APK`.
6. Extract the ZIP and install the debug APK on an Android device that supports OpenGL ES 2.0.

You can also start a build manually from Actions using **Run workflow**.

## Technical details
- Native Android Java + OpenGL ES 2.0.
- Android minimum version: Android 6.0 (API 23).
- Debug application ID: `com.saji.grandcity.debug`.
- No network permission is requested.

## Important scope note
This first build is intentionally a small prototype. It does not yet include a large streamed map, detailed character animations, vehicle driving, missions, interiors, sound, save files, or production-level graphics. Those need to be developed and tested as separate stages. The 2 GB target is a future content budget, not the size of this prototype.
