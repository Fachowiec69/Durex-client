# Discord Ticket Bot

Bot do obsługi ticketów z dropdown menu kategorii.

## Instalacja

1. Zainstaluj zależności:
```bash
pip install -r requirements.txt
```

2. Skopiuj `.env.example` do `.env` i wpisz token bota:
```bash
cp .env.example .env
```

3. Edytuj `.env` i wstaw swój token z [Discord Developer Portal](https://discord.com/developers/applications)

## Uruchomienie

```bash
python bot.py
```

## Użycie

1. Użyj komendy `/ticket-setup` na kanale gdzie chcesz panel ticketów
2. Użytkownicy wybierają kategorię z dropdown
3. Bot tworzy prywatny kanał dla ticketa
4. Przycisk "Zamknij ticket" usuwa kanał

## Kategorie

- 🍃 Zakup
- 🎁 Odbiór nagrody
- 🏛️ Sprzedaż
- 🔄 Wymiana
- ❓ Inne

## Wymagania

- Python 3.8+
- discord.py 2.3.0+
- Uprawnienia bota: Manage Channels, Send Messages, Embed Links
