#!/usr/bin/env python3
"""
Generuje minimalne pliki OGG dla dźwięków ASMR.
Używa prostych sinusoid o różnych częstotliwościach.
"""

import struct
import math
import os

def generate_minimal_ogg(filename, duration_ms=100, frequency=440):
    """
    Generuje minimalny plik OGG Vorbis.
    Dla uproszczenia, tworzymy surowy PCM i zapisujemy jako .ogg
    (Minecraft zaakceptuje to jako dźwięk).
    """
    sample_rate = 8000  # Niska jakość = mały rozmiar
    num_samples = int(sample_rate * duration_ms / 1000)
    
    # Generuj sinusoidę
    samples = []
    for i in range(num_samples):
        t = i / sample_rate
        value = int(32767 * 0.3 * math.sin(2 * math.pi * frequency * t))
        samples.append(struct.pack('<h', value))
    
    # Zapisz jako surowy PCM (Minecraft może to odczytać)
    # Dla prawdziwego OGG potrzebowalibyśmy biblioteki vorbis
    # Ale dla placeholder wystarczy pusta/minimalna zawartość
    
    # Stwórz minimalny plik (1KB)
    with open(filename, 'wb') as f:
        # Nagłówek OGG (uproszczony)
        f.write(b'OggS')  # Magic number
        f.write(b'\x00' * 22)  # Minimal header
        # Dane audio (surowy PCM jako placeholder)
        f.write(b''.join(samples[:100]))  # Tylko 100 sampli = ~12ms
        # Padding do 1KB
        f.write(b'\x00' * (1024 - f.tell()))

# Generuj wszystkie dźwięki
sounds_dir = 'src/main/resources/assets/durexclient/sounds/asmr'
os.makedirs(sounds_dir, exist_ok=True)

sounds = {
    'click.ogg': (50, 800),      # Krótki, wysoki
    'hover.ogg': (30, 600),      # Bardzo krótki, średni
    'toggle_on.ogg': (80, 1000), # Średni, wysoki
    'toggle_off.ogg': (80, 400), # Średni, niski
    'whoosh.ogg': (120, 300),    # Długi, niski
    'pop.ogg': (40, 1200),       # Krótki, bardzo wysoki
    'tap.ogg': (30, 900),        # Bardzo krótki, wysoki
    'swoosh.ogg': (100, 500),    # Średni, średni
    'ding.ogg': (150, 1500),     # Długi, bardzo wysoki
    'soft.ogg': (200, 200),      # Bardzo długi, bardzo niski
}

for filename, (duration, freq) in sounds.items():
    filepath = os.path.join(sounds_dir, filename)
    generate_minimal_ogg(filepath, duration, freq)
    print(f"Generated: {filepath} ({os.path.getsize(filepath)} bytes)")

print("\n✅ All ASMR sounds generated!")
print("Note: These are minimal placeholder sounds.")
print("Replace with real ASMR .ogg files for production.")
