# Kick TV (Android TV 14)

Lekka i w pełni funkcjonalna aplikacja dedykowana na telewizory z Android TV / Google TV (Android 14) do oglądania transmisji na żywo z serwisu **Kick.com**.

## Główne cechy
- **Silnik Chromium (WebView)**: Emulacja Desktop Chrome (User-Agent Windows/Chrome), dzięki czemu Kick serwuje pełny odtwarzacz HTML5 (1080p60, HLS, czat na żywo) bez blokad i bez wymuszania instalacji aplikacji mobilnej.
- **Wirtualny kursor / Air-Mouse (DPAD Remote Navigation)**: Płynny wskaźnik myszy sterowany strzałkami pilota z akceleracją, pozwalający bezproblemowo klikać dowolne przyciski na stronie (zmiana jakości, teatr, wyciszenie, czat, kategorie). Auto-ukrywanie kursora po 4 sekundach bezczynności.
- **Wsparcie dla pilota TV**:
  - `DPAD (Góra/Dół/Lewo/Prawo)`: Płynne poruszanie kursorem myszy / przewijanie strony na krawędziach.
  - `DPAD Center / OK`: Kliknięcie elementu pod kursorem.
  - `MENU` / `BOOKMARK` / `INFO`: Otwarcie podręcznego TV Menu.
  - `PLAY` / `PAUSE` / `MEDIA_PLAY_PAUSE`: Sterowanie odtwarzaniem wideo.
  - `BACK`: Powrót do poprzedniej strony / wyjście z pełnego ekranu wideo / podwójne kliknięcie wychodzi z aplikacji.
- **Podręczne TV Menu (HUD)**:
  - Szybki powrót do strony głównej Kick.
  - Przeglądanie kategorii.
  - Wyszukiwarka / bezpośrednie przejście do kanału (wpisanie nicku).
  - ⭐ **Ulubione kanały**: Zapisywanie ulubionych streamerów w pamięci aplikacji i szybkie przełączanie.
  - Przełącznik czatu (ukryj/pokaż).
  - Wymuszenie trybu kinowego / pełnego ekranu.
  - Przełącznik: Wirtualny wskaźnik myszy vs. natywny DPAD.
- **Optymalizacje TV**:
  - `android:hardwareAccelerated="true"` i obsługa wideo 60 FPS.
  - `FLAG_KEEP_SCREEN_ON`: Ekran nie wygasza się podczas oglądania.
  - Blokowanie uciążliwych banerów cookie i powiadomień o aplikacji mobilnej.
  - Dedykowany TV Banner (320x180) i kafel w launcherze Android TV Leanback.
  - Waga pliku APK to zaledwie ~44 KB!

## Budowanie i instalacja

### Szybka instalacja przez skrypt:
```bash
cd /home/kszunio/kick-tv
TV_HOST=192.168.1.XX ./build-and-install.sh
```

### Ręczna instalacja:
Plik APK znajduje się w:
```
/home/kszunio/kick-tv/app/build/outputs/apk/debug/app-debug.apk
```

Możesz zainstalować go bezpośrednio przez ADB:
```bash
adb connect 192.168.1.XX:5555
adb install -r /home/kszunio/kick-tv/app/build/outputs/apk/debug/app-debug.apk
```
