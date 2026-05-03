# 🔧 Debug Testing - LeverCobweb

## 🚨 Problem: "Nic nie działa"

### ✅ Co zostało naprawione:

1. **Zmieniono z Mixin na Fabric API Event** - Unikamy konfliktów z innymi modami
2. **Dodano debug logi** - Teraz widzimy co się dzieje
3. **Moduł domyślnie włączony** - Nie trzeba go włączać w GUI

### 🧪 Jak przetestować:

#### **Krok 1: Sprawdź logi**
Po uruchomieniu Minecraft powinieneś zobaczyć w logach:
```
[LeverCobweb] Module initialized!
```

#### **Krok 2: Test podstawowy**
1. **Weź pajęczynę** do głównej ręki
2. **Weź wiadro wody** do ekwipunku  
3. **Kliknij PPM** na jakiś blok
4. **Sprawdź logi** - powinieneś zobaczyć:
```
[LeverCobweb] handleInteractBlock called! enabled=true
```

#### **Krok 3: Test GUI**
1. **Naciśnij Right Shift** (otwórz GUI)
2. **Znajdź moduł LeverCobweb** (🕸️)
3. **Sprawdź czy jest ON** (zielony)
4. **Kliknij prawym** aby rozwinąć opcje

### 🔍 Diagnostyka problemów:

#### **Jeśli nie widzisz "Module initialized!"**
- Mod się nie ładuje poprawnie
- Sprawdź czy plik JAR jest w folderze `mods/`
- Sprawdź czy masz Fabric Loader

#### **Jeśli nie widzisz "handleInteractBlock called!"**
- Fabric API event nie działa
- Sprawdź czy masz najnowszą wersję Fabric API
- Sprawdź czy nie ma konfliktów z innymi modami

#### **Jeśli widzisz "Early return"**
- Moduł jest wyłączony (`enabled=false`)
- Używasz lewej ręki zamiast prawej (`hand=OFF_HAND`)
- Masz włączony internal flag (`internal=true`)

### 🎯 Szybki test:

```
1. Uruchom Minecraft z modem
2. Wejdź do świata (creative lub survival)
3. Weź pajęczynę + wiadro wody
4. Kliknij PPM na ziemię
5. Sprawdź logi w konsoli
```

### 📋 Checklist:

- [ ] Minecraft 1.21.4
- [ ] Fabric Loader zainstalowany  
- [ ] Fabric API zainstalowany
- [ ] DurexClient JAR w folderze mods/
- [ ] Pajęczyna w głównej ręce
- [ ] Wiadro wody w ekwipunku
- [ ] Kliknięcie PPM (nie LPM!)

### 🆘 Jeśli nadal nie działa:

1. **Wyślij logi** z konsoli Minecraft
2. **Sprawdź listę modów** - może jest konflikt
3. **Przetestuj bez innych modów** - tylko Fabric API + DurexClient
4. **Sprawdź wersję Fabric API** - powinna być kompatybilna z 1.21.4

---

**Nowa wersja używa Fabric API Event zamiast Mixin - powinno być bardziej stabilne!** 🚀