# 🕸️ LeverCobweb Module - Przewodnik

## Opis
Moduł **LeverCobweb** dodaje zaawansowaną automatyzację dla kombinacji dźwigni (lever) i pajęczyn (cobweb). Automatycznie wykonuje sekwencję: **płyn → blok → zabranie płynu**, co znacznie przyspiesza stawianie bloków w trudnych miejscach.

## ✨ Główne funkcje

### 🔄 Automatyczna sekwencja
Gdy klikniesz prawym przyciskiem z blokiem w ręce, mod automatycznie:
1. **Znajdzie wiadro z płynem** w ekwipunku (woda/lawa)
2. **Postawi płyn** w odpowiednim miejscu
3. **Postawi blok** (np. pajęczynę)
4. **Zabierze płyn** z powrotem do wiadra
5. **Przełączy na najlepszy miecz** (opcjonalnie)

### 🎯 Lever Hold (Trzymanie dźwigni)
- Automatyczne trzymanie dźwigni w pozycji aktywnej
- Wymaga dźwigni w **offhand** (lewa ręka)
- Automatyczne **sneaking** gdy potrzebne

### ⚔️ Auto Sword Switch
- Automatyczne przełączanie na najlepszy miecz po zakończeniu sekwencji
- Priorytet: Netherite → Diamond → Iron → Gold → Stone → Wood

## 🎮 Jak używać

### Podstawowe użycie
1. **Włącz moduł** w GUI (Right Shift → LeverCobweb → ON)
2. **Weź blok** do głównej ręki (np. pajęczynę)
3. **Upewnij się** że masz wiadro z wodą/lawą w ekwipunku
4. **Kliknij prawym** na miejsce gdzie chcesz postawić blok
5. **Mod automatycznie** wykona całą sekwencję

### Lever Hold
1. **Włącz "Hold Lever"** w ustawieniach modułu
2. **Weź dźwignię** do offhand (lewa ręka)
3. **Trzymaj Use Key** (domyślnie PPM) na dźwigni
4. **Mod będzie automatycznie** aktywować dźwignię

## ⚙️ Konfiguracja

### Dostępne opcje w GUI:
- **Module Enabled** - Włącza/wyłącza cały moduł
- **Hold Lever** - Włącza/wyłącza funkcję trzymania dźwigni
- **Auto Sword** - Włącza/wyłącza automatyczne przełączanie na miecz

### Zaawansowane ustawienia (w kodzie):
```java
// Liczba prób postawienia bloku
private static final int PLACE_RETRIES = 3;

// Liczba prób postawienia płynu
private static final int FLUID_PLACE_ATTEMPTS = 4;

// Liczba prób zabrania płynu
private static final int FLUID_PICKUP_ATTEMPTS = 8;

// Opóźnienia (w tickach)
private static final int RETRY_DELAY_TICKS = 2;
private static final int SETTLE_DELAY_TICKS = 1;
private static final int POST_PICKUP_DELAY_TICKS = 1;
```

## 🔧 Jak to działa (technicznie)

### Wykrywanie sytuacji
Mod automatycznie wykrywa:
- **Kliknięcie na dźwignię** → tworzy plan automatyzacji
- **Kliknięcie na pajęczynę** → szuka miejsca na dźwignię
- **Normalne stawianie bloku** → sprawdza czy można zautomatyzować

### Etapy sekwencji
1. **SELECT_FLUID** - Wybiera wiadro z płynem
2. **USE_FLUID** - Stawia płyn
3. **WAIT_FOR_EMPTY_BUCKET** - Czeka aż wiadro będzie puste
4. **PICKUP_FLUID** - Zabiera płyn z powrotem
5. **WAIT_FOR_FILLED_BUCKET** - Czeka aż wiadro będzie pełne
6. **RESTORE_PLACEMENT_ITEM** - Przywraca oryginalny przedmiot
7. **PLACE_BLOCK** - Stawia docelowy blok

### Stabilizacja ruchu
Mod czeka na stabilizację gracza przed wykonaniem akcji:
- Sprawdza prędkość poziomą i pionową
- Wykrywa czy gracz jest w pajęczynie
- Automatycznie dostosowuje timing

## 🎯 Przypadki użycia

### PvP
- **Szybkie stawianie pajęczyn** w walce
- **Blokowanie przeciwników** w trudnych miejscach
- **Automatyczne przełączanie na miecz** po postawieniu

### Budowanie
- **Stawianie bloków w wodzie/lawie** bez zostawiania płynu
- **Precyzyjne umieszczanie** w trudno dostępnych miejscach
- **Automatyzacja powtarzalnych czynności**

### Eksploracja
- **Szybkie tworzenie platform** nad lawą
- **Blokowanie mobów** pajęczynami
- **Efektywne poruszanie się** po trudnym terenie

## ⚠️ Ważne informacje

### Wymagania
- **Minecraft 1.21.4** z Fabric
- **Wiadro z płynem** w ekwipunku (woda lub lawa)
- **Blok do postawienia** w głównej ręce

### Ograniczenia
- Działa tylko z **blokami** (nie z przedmiotami)
- Wymaga **płynu w ekwipunku** do automatyzacji
- **Lever Hold** wymaga dźwigni w offhand

### Kompatybilność
- **Bezpieczny** - nie modyfikuje podstawowej mechaniki gry
- **Wydajny** - minimalne obciążenie FPS
- **Stabilny** - zaawansowane mechanizmy retry i error handling

## 🐛 Rozwiązywanie problemów

### Mod nie działa
1. Sprawdź czy moduł jest **włączony** w GUI
2. Upewnij się że masz **wiadro z płynem**
3. Sprawdź czy trzymasz **blok** (nie przedmiot)

### Sekwencja się przerywa
1. **Nie ruszaj się** podczas wykonywania sekwencji
2. Sprawdź czy masz **wystarczająco miejsca** w ekwipunku
3. Upewnij się że **nie ma przeszkód** w miejscu stawiania

### Lever Hold nie działa
1. Sprawdź czy masz **dźwignię w offhand**
2. Upewnij się że **Hold Lever** jest włączone
3. **Trzymaj Use Key** na dźwigni

## 📝 Changelog

### v1.0.0
- ✅ Podstawowa automatyzacja Lever + Cobweb
- ✅ Funkcja Hold Lever
- ✅ Automatyczne przełączanie na miecz
- ✅ Zaawansowana stabilizacja ruchu
- ✅ Integracja z GUI DurexClient
- ✅ Obsługa różnych typów płynów (woda/lawa)
- ✅ System retry i error handling

---

**Autor:** DurexClient Team  
**Wersja:** 1.0.0  
**Data:** 2024