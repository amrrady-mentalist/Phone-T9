# Phone (native Kotlin)

Native Jetpack Compose port of the "Phone" web app: a normal-looking dialer with a hidden T9 engine.

## The secret
- Type digits on the keypad, then tap **Recents** (bottom left). The "Smart Results Engine" popup shows up to 6 words.
- Tapping a word POSTs `{"value": word, "sequence": digits}` to your API.
- The green **Call** button flashes the digits and POSTs the top word (or `"unknown"`).
- API URL: three-dot menu (top right). Empty URL = nothing is sent.

## What changed from the web app
| Web app | Kotlin app |
|---|---|
| Dictionary downloaded from GitHub at start | `app/src/main/assets/words_alpha.txt` (370,105 words, 4.2 MB) bundled, fully offline |
| Import contacts from .vcf / .csv | Reads the phone's contacts (READ_CONTACTS, asked on first launch) |
| `fetch` to API | `ApiClient` (HttpURLConnection). `ApiClient.get(url)` is ready for receiving data |
| `localStorage` | SharedPreferences |
| Call / Text buttons showed alerts | Open the real dialer / SMS app (the API event is still sent first) |
| Edit button showed an alert | Opens the real contact editor |

## T9 engine
`t9/T9Engine.kt` has no Android imports. It reproduces the original ranking exactly, including JS object
key ordering for ties and one JS quirk (the word "constructor" can never appear).
`app/src/test/resources/golden.tsv` holds 1,185 sequences produced by running the ORIGINAL JavaScript against
the full dictionary; `T9EngineTest` checks count, md5 of the whole ranked list and the first 25 results.

    ./gradlew testDebugUnitTest
    ./gradlew assembleDebug

## Signing and updates
The signing key is NOT in the repo (the repo is public). It lives in GitHub Secrets:
`PHONE_KEYSTORE_BASE64`, `PHONE_KEYSTORE_PASSWORD`, `PHONE_KEY_PASSWORD`. CI restores it to `app/phone.jks`
at build time (`*.jks` is git-ignored). Builds made without the secrets fall back to the debug key.
CI sets `versionCode` to the run number, so each build installs over the previous one with no uninstall.
Keep a private backup of `phone.jks`: lose it and you are back to uninstalling.

- `.github/workflows/build.yml` runs on every push: unit tests, then a release APK uploaded as an artifact.
- `.github/workflows/release.yml` (Actions tab, Run workflow) publishes the signed APK as a GitHub Release,
  the easiest way to download it on your phone.

## Notes
- Same applicationId as the web version (`com.arady.phone`). Uninstall the old web-wrapper APK ONCE (different signature); every build after that updates in place.
- `usesCleartextTraffic` is on, so an `http://` API URL works.
- minSdk 26, targetSdk 35, Compose BOM 2024.10.01, Kotlin 2.0.21, AGP 8.5.2.
