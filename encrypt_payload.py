#!/usr/bin/env python3
"""
Skrypt do szyfrowania LicenseModule.class przed wrzuceniem na Cloudflare Worker.

Użycie:
    python3 encrypt_payload.py <sciezka_do_LicenseModule.class lub .b64>

Wynik:
    encrypted_payload.b64 - gotowy do wrzucenia na workera
"""

import sys
import base64
import hashlib
from Crypto.Cipher import AES

# ── Klucz payloadu (warstwa 2) - zakodowany w atlas_sync.bin przez envelope ──
# Te wartości muszą zgadzać się z tym co PayloadCipher odczytuje z atlas_sync.bin
PAYLOAD_KEY = bytes.fromhex("4083ae3581c78235136c48b75f7549f5da582b763646aba2513fa689c6c36e87")
PAYLOAD_IV  = bytes.fromhex("7795a9c057128fd1c82a2fe735c1d0ee")

def encrypt(class_bytes: bytes) -> bytes:
    # Warstwa 2: AES-CTR(PAYLOAD_KEY, PAYLOAD_IV)
    cipher = AES.new(PAYLOAD_KEY, AES.MODE_CTR, nonce=b'', initial_value=PAYLOAD_IV)
    return cipher.encrypt(class_bytes)

def main():
    if len(sys.argv) < 2:
        print("Użycie: python3 encrypt_payload.py <LicenseModule.class lub plik.b64>")
        sys.exit(1)

    path = sys.argv[1]
    raw = open(path, "rb").read()

    # Jeśli to base64 (plik .b64 lub tekst), zdekoduj najpierw
    if path.endswith(".b64") or path.endswith(".txt"):
        clean = raw.strip()
        if clean[:1] == b':':
            clean = clean[1:]
        class_bytes = base64.b64decode(clean)
    else:
        class_bytes = raw

    # Sprawdź magic
    if class_bytes[:4] != b'\xca\xfe\xba\xbe':
        print(f"[!] Ostrzeżenie: magic = {class_bytes[:4].hex()}, oczekiwano cafebabe")
    else:
        print(f"[+] Magic OK: cafebabe")

    print(f"[*] Rozmiar klasy: {len(class_bytes)} bajtów")

    encrypted = encrypt(class_bytes)
    encoded   = base64.b64encode(encrypted).decode("ascii")

    out_path = "encrypted_payload.b64"
    with open(out_path, "w") as f:
        f.write(encoded)

    print(f"[+] Zaszyfrowano -> {out_path} ({len(encrypted)} bajtów)")
    print(f"[+] Wrzuć zawartość {out_path} na Cloudflare Worker pod klucz payload_class")

if __name__ == "__main__":
    main()
