# 🔊 Jak Zastąpić Dźwięki Prawdziwymi ASMR

## Obecny Stan
Dźwięki to minimalne placeholdery (1KB każdy, 10KB total).
Działają, ale nie są prawdziwymi ASMR dźwiękami.

## Krok 1: Pobierz Dźwięki ASMR

### Źródła:
- **YouTube**: Szukaj "ASMR sounds pack", "UI sounds ASMR"
- **Freesound.org**: Darmowe dźwięki (licencja CC)
- **Zapsplat.com**: Darmowe sound effects
- **Pixabay**: Darmowe audio

### Polecane Wyszukiwania:
```
- "ASMR keyboard click"
- "ASMR mouse click"
- "ASMR pop sound"
- "ASMR tap sound"
- "ASMR whoosh"
- "satisfying click sound"
- "soft tap ASMR"
```

## Krok 2: Konwertuj do OGG

Minecraft wymaga formatu **OGG Vorbis**.

### Używając FFmpeg:
```bash
# Instalacja FFmpeg (jeśli nie masz)
# Ubuntu/Debian: sudo apt install ffmpeg
# macOS: brew install ffmpeg
# Windows: Pobierz z ffmpeg.org

# Konwersja MP3 → OGG
ffmpeg -i input.mp3 -c:a libvorbis -q:a 4 output.ogg

# Konwersja WAV → OGG
ffmpeg -i input.wav -c:a libvorbis -q:a 4 output.ogg

# Batch konwersja (wszystkie MP3 w folderze)
for f in *.mp3; do ffmpeg -i "$f" -c:a libvorbis -q:a 4 "${f%.mp3}.ogg"; done
```

### Parametry:
- `-q:a 4` = jakość (0=najlepsza, 10=najgorsza). 4 to dobry balans.
- Dla mniejszych plików: `-q:a 6` lub `-b:a 64k`

### Online Konwertery:
- **CloudConvert.com** - obsługuje OGG
- **Online-Convert.com** - MP3/WAV → OGG
- **Convertio.co** - batch conversion

## Krok 3: Przytnij Dźwięki (Opcjonalne)

Dźwięki UI powinny być **krótkie** (50-200ms).

### Używając FFmpeg:
```bash
# Przytnij do pierwszych 200ms
ffmpeg -i input.ogg -t 0.2 -c copy output.ogg

# Przytnij od 1s do 1.2s (200ms)
ffmpeg -i input.ogg -ss 1.0 -t 0.2 -c copy output.ogg
```

### Używając Audacity (GUI):
1. Otwórz plik w Audacity
2. Zaznacz fragment (50-200ms)
3. File → Export → Export as OGG Vorbis
4. Quality: 4-6

## Krok 4: Zmień Nazwę Plików

Pliki muszą mieć **dokładnie** te nazwy:

```
click.ogg       - Kliknięcie (50-80ms)
hover.ogg       - Najechanie myszką (30-50ms)
toggle_on.ogg   - Włączenie modułu (80-120ms)
toggle_off.ogg  - Wyłączenie modułu (80-120ms)
whoosh.ogg      - Przesunięcie (100-150ms)
pop.ogg         - Pop (40-80ms)
tap.ogg         - Stukanie (30-60ms)
swoosh.ogg      - Swoosh (100-150ms)
ding.ogg        - Dzwonek (150-250ms)
soft.ogg        - Miękki dźwięk (150-300ms)
```

## Krok 5: Zastąp Pliki

```bash
# Backup starych plików (opcjonalne)
cp -r src/main/resources/assets/durexclient/sounds/asmr src/main/resources/assets/durexclient/sounds/asmr.backup

# Skopiuj nowe dźwięki
cp /path/to/your/sounds/*.ogg src/main/resources/assets/durexclient/sounds/asmr/
```

## Krok 6: Rebuild

```bash
./gradlew clean build
```

## Krok 7: Testuj

1. Uruchom Minecraft z modem
2. Otwórz GUI (`Right Shift`)
3. Kliknij **Settings** → **Dźwięki**
4. Kliknij **▶** przy każdym dźwięku żeby przetestować
5. Toggle moduły w GUI żeby usłyszeć `toggle_on` i `toggle_off`

## 📏 Zalecane Rozmiary

Dla małego JAR (<500KB):

| Dźwięk | Długość | Rozmiar | Jakość |
|--------|---------|---------|--------|
| click  | 50ms    | 2-5KB   | q:a 6  |
| hover  | 30ms    | 1-3KB   | q:a 6  |
| toggle_on | 80ms | 3-6KB   | q:a 5  |
| toggle_off | 80ms | 3-6KB  | q:a 5  |
| whoosh | 120ms   | 5-8KB   | q:a 5  |
| pop    | 40ms    | 2-4KB   | q:a 6  |
| tap    | 30ms    | 1-3KB   | q:a 6  |
| swoosh | 100ms   | 4-7KB   | q:a 5  |
| ding   | 150ms   | 6-10KB  | q:a 5  |
| soft   | 200ms   | 8-12KB  | q:a 5  |

**Total**: ~35-65KB (nadal <500KB JAR)

## 🎯 Przykładowy Workflow

```bash
# 1. Pobierz dźwięki z YouTube
yt-dlp -x --audio-format mp3 "https://youtube.com/watch?v=ASMR_SOUNDS"

# 2. Konwertuj do OGG
ffmpeg -i asmr_sounds.mp3 -ss 0:00 -t 0.05 -c:a libvorbis -q:a 6 click.ogg
ffmpeg -i asmr_sounds.mp3 -ss 0:10 -t 0.03 -c:a libvorbis -q:a 6 hover.ogg
# ... itd dla każdego dźwięku

# 3. Skopiuj do projektu
cp *.ogg src/main/resources/assets/durexclient/sounds/asmr/

# 4. Rebuild
./gradlew clean build

# 5. Testuj
java -jar build/libs/durex-client-1.0.0-Legit.jar
```

## 🔍 Sprawdzanie Rozmiaru

```bash
# Rozmiar pojedynczego pliku
ls -lh src/main/resources/assets/durexclient/sounds/asmr/click.ogg

# Rozmiar wszystkich dźwięków
du -sh src/main/resources/assets/durexclient/sounds/asmr/

# Rozmiar finalnego JAR
ls -lh build/libs/durex-client-1.0.0-Legit.jar
```

## ⚠️ Uwagi

1. **Format**: Tylko OGG Vorbis (nie OGG Opus!)
2. **Nazwy**: Dokładnie jak w liście (małe litery, bez spacji)
3. **Długość**: Krótkie dźwięki (50-200ms) dla UI
4. **Jakość**: q:a 5-6 dla małych plików
5. **Licencja**: Upewnij się że masz prawo używać dźwięków

## 🎵 Polecane Dźwięki

### Click
- Mechanical keyboard switch
- Mouse click
- Pen click

### Hover
- Soft air puff
- Gentle whoosh
- Paper slide

### Toggle On
- Satisfying snap
- Light switch on
- Pop cork

### Toggle Off
- Soft thud
- Light switch off
- Muted click

### Whoosh
- Fast air movement
- Fabric swish
- Paper whoosh

### Pop
- Bubble pop
- Balloon pop
- Cork pop

### Tap
- Finger tap on wood
- Gentle knock
- Nail tap

### Swoosh
- Fabric movement
- Paper slide
- Soft swish

### Ding
- Small bell
- Wind chime
- Glass clink

### Soft
- Pillow touch
- Cotton movement
- Soft fabric

---

**Gotowe!** Teraz masz prawdziwe ASMR dźwięki w kliencie. 🎉
