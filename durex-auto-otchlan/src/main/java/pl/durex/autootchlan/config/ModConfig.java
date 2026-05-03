package pl.durex.autootchlan.config;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    // Komenda otwierająca otchłań
    public String command = "/otchlan";

    // Slot "następna strona" (zielony barwnik) — domyślnie slot 53 (ostatni w 6-rzędowym GUI)
    public int nextPageSlot = 53;

    // Slot "poprzednia strona" — domyślnie slot 45
    public int prevPageSlot = 45;

    // Ile ms czekać po otwarciu GUI przed klikaniem
    public int openDelayMs = 100;

    // Ile ms między kliknięciami slotów
    public int clickDelayMs = 0;

    // Ile ms czekać po przejściu strony
    public int pageDelayMs = 50;

    // Czy wyrzucać WSZYSTKIE itemy (nie tylko priorytety)
    public boolean grabAll = true;

    // Priorytety — te itemy wyrzucane pierwsze
    public List<PriorityEntry> priorities = new ArrayList<>();

    // Spam /otchlan przy countdown = 1
    public boolean spamOnOne = true;

    // Ile razy spamować komendę przy countdown = 1
    public int spamCount = 20;

    // Interwał spamu w ms
    public int spamIntervalMs = 50;
}
