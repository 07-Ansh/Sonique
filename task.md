# Sonique Roadmap: Upstream Feature Integration (Local `freshfeel` Branch)

> **Workflow Rule**: Strictly ONE item at a time. Read upstream code, adapt cleanly to Sonique namespaces, verify compilation (`./gradlew :composeApp:compileDebugKotlinAndroid`), commit locally, and pause.

---

## 1. Core Streaming & Network Engine (CURRENT FOCUS)
- [x] **1.1 Branch Setup**: Create `freshfeel` branch from `master`
- [x] **1.2 Cipher Decoding Engine**: Port local cipher decoder & centralized ITAG resolver in `core:service` / `core:data` for robust streaming without throttling
- [x] **1.3 Audio Format Options**: Add explicit Opus vs AAC and 256kbps/320kbps stream quality preferences in DataStore & stream picker
- [x] **1.4 SponsorBlock Skip Logic**: Port short-segment loop fix for brief sponsored segments
- [x] **1.5 Local Song Cache**: Optimize local audio cache verification prior to API fetching
- [x] **1.6 Extraction Source Tagging**: Tag media extraction provider (PipePipe, BravePipe) in song entity metadata and track info sheet

---

## 2. Audio Engine & DSP Effects
- [x] **2.1 Delay DSP Effect**: Port Delay audio processor in `core:media` and add settings controls in `SettingScreen.kt`
- [x] **2.2 Reverb DSP Effect**: Port Reverb audio processor in `core:media` and add settings controls in `SettingScreen.kt`
- [x] **2.3 DJ AutoMix & Equal-Power Crossfade**: Implement equal-power crossfade curve with front-loaded BPM ramp
- [x] **2.4 Dynamic Volume Normalization**: Integrate updated audio normalization filters
- [x] **2.5 Audio Metrics Analysis**: Extract & render track BPM, musical key, and scale in track info sheet
- [x] **2.6 System Equalizer (`OpenEq`)**: Verify Android system equalizer launcher integration

---

## 3. Advanced Lyrics Engine
- [x] **3.1 Travelling Word Light**: Wall-clock based karaoke word-by-word wipe/glow animation in `LyricsView.kt`
- [x] **3.2 Interlude Progress Dots**: Render animated progress dots (`...`) during instrumental breaks
- [x] **3.3 Lyrics Timing Offset Slider**: Dynamic slider in lyrics sheet (+/- seconds) for on-the-fly sync correction
- [x] **3.4 12-Language Romanization**: Transliteration pipeline with on-demand Japanese dictionary downloading
- [x] **3.5 Share Lyrics as Image**: Card generator canvas exporter for sharing selected lyric lines
- [x] **3.6 AI Lyrics Translation**: OpenAI-compatible custom API endpoint integration for lyric translations
- [x] **3.7 BetterLyrics Engine**: Integration of BetterLyrics as a supplementary lyrics provider

---

## 4. Now Playing & Visual Experience (SKIPPED)
- [ ] ~~**4.1 Apple Music-Style Player Screen**~~ (Skipped)
- [ ] ~~**4.2 Spotify-Style Horizontal Pager**~~ (Skipped)
- [ ] ~~**4.3 Spotify Canvas & Animated Artwork**~~ (Skipped)
- [ ] ~~**4.4 Hand-Drawn Playback Indicators**~~ (Skipped)
- [ ] ~~**4.5 Resizable Glance Widgets**~~ (Skipped)

---

## 5. UI Screens & Navigation (SKIPPED)
- [ ] ~~**5.1 Press-to-Bulge Liquid Glass**~~ (Skipped)
- [ ] ~~**5.2 Immersive Artist Screen**~~ (Skipped)
- [ ] ~~**5.3 Offline Error Screen**~~ (Skipped)
- [ ] ~~**5.4 Podcast Screen**~~ (Skipped)
- [ ] ~~**5.5 Multi-Select & In-Playlist Search**~~ (Skipped)
- [ ] ~~**5.6 Custom Playlist Cover Crop**~~ (Skipped)

---

## 6. Analytics, Wrapped & Social
- [ ] ~~**6.1 Year-in-Review ("Wrapped")**~~ (Skipped)
- [ ] ~~**6.2 Redesigned Analytics**~~ (Skipped)
- [ ] ~~**6.3 Auto Backup**~~ (Skipped)
- [x] **6.4 Listen Together**: Synchronized multi-device friend listening engine
- [ ] ~~**6.5 Last.fm Scrobbler**~~ (Skipped)
