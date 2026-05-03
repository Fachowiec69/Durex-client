#!/usr/bin/env python3
import base64
import requests
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad
import hashlib

# Pobierz aktualny payload z serwera
response = requests.get("https://notoggogolnie.xx570186.workers.dev/payload")
server_payload = response.text.strip()
encoded_bytes = base64.b64decode(server_payload)

print(f"Encoded size: {len(encoded_bytes)} bytes")
print(f"First 32 bytes (hex): {encoded_bytes[:32].hex()}")

# Stary system używał 3 kluczy XOR-owanych razem
# PayloadKeyA - nie mamy
# PayloadKeyB: [39, 121, 24, 188, 94, 16, 219, 172, 159, 110, 124, 155, 109, 132, 164, 197, 103, 71, 71]
# PayloadKeyC: [165, 26, 17, 193, 67, 90, 228, 109, 227, 13, 113, 223, 167, 164, 144, 232, 133, 176, 140]

# Może stary system używał AES z kluczem z PayloadKeyA?
# Sprawdźmy czy to jest AES-128 lub AES-256

# Spróbuj różnych kluczy AES
test_keys = [
    b"DurexClientKey24",  # 16 bytes
    b"DurexClientKey2024",  # 18 bytes - nie AES
    b"DurexKey12345678",  # 16 bytes
    b"DurexClientKey20",  # 16 bytes
]

for key in test_keys:
    if len(key) not in [16, 24, 32]:
        continue
    try:
        # Spróbuj AES-CBC z IV = pierwsze 16 bajtów
        iv = encoded_bytes[:16]
        ciphertext = encoded_bytes[16:]
        
        cipher = AES.new(key, AES.MODE_CBC, iv)
        decrypted = unpad(cipher.decrypt(ciphertext), AES.block_size)
        
        magic = int.from_bytes(decrypted[:4], 'big')
        if magic == 0xCAFEBABE:
            print(f"✅ Found AES key: {key}")
            break
    except Exception as e:
        pass

print("No AES key found with simple guesses")
print("\nThe payload on server uses old AES encryption.")
print("Solution: Upload new payload_raw.txt (plain base64) to server")
print("Or: Deploy updated Cloudflare Worker with /upload endpoint")
