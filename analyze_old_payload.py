#!/usr/bin/env python3
import base64
import requests

# Pobierz aktualny payload z serwera
response = requests.get("https://notoggogolnie.xx570186.workers.dev/payload")
server_payload = response.text.strip()

encoded_bytes = base64.b64decode(server_payload)
print(f"Encoded size: {len(encoded_bytes)} bytes")
print(f"First 32 bytes (hex): {encoded_bytes[:32].hex()}")

# Stary system: PayloadKeyA + PayloadKeyB + PayloadKeyC
# Z kodu który był wcześniej - klucze były:
# PayloadKeyA: nie mamy już
# PayloadKeyB: [39, 121, 24, 188, 94, 16, 219, 172, 159, 110, 124, 155, 109, 132, 164, 197, 103, 71, 71]
# PayloadKeyC: [165, 26, 17, 193, 67, 90, 228, 109, 227, 13, 113, 223, 167, 164, 144, 232, 133, 176, 140]

key_b = bytes([39, 121, 24, 188, 94, 16, 219, 172, 159, 110, 124, 155, 109, 132, 164, 197, 103, 71, 71])
key_c = bytes([165, 26, 17, 193, 67, 90, 228, 109, 227, 13, 113, 223, 167, 164, 144, 232, 133, 176, 140])

# Spróbuj XOR z key_b
result_b = bytearray()
for i, byte in enumerate(encoded_bytes):
    result_b.append(byte ^ key_b[i % len(key_b)])

magic_b = int.from_bytes(result_b[:4], 'big')
print(f"XOR with key_b magic: 0x{magic_b:08x}")

# Spróbuj XOR z key_c
result_c = bytearray()
for i, byte in enumerate(encoded_bytes):
    result_c.append(byte ^ key_c[i % len(key_c)])

magic_c = int.from_bytes(result_c[:4], 'big')
print(f"XOR with key_c magic: 0x{magic_c:08x}")

# Spróbuj kombinację key_b XOR key_c
combined_key = bytes([key_b[i] ^ key_c[i] for i in range(len(key_b))])
result_combined = bytearray()
for i, byte in enumerate(encoded_bytes):
    result_combined.append(byte ^ combined_key[i % len(combined_key)])

magic_combined = int.from_bytes(result_combined[:4], 'big')
print(f"XOR with combined key magic: 0x{magic_combined:08x}")

# Sprawdź czy jest header (pierwsze 16 bajtów to padding)
# Stary kod: payload = encrypted[16:]
payload_no_header = encoded_bytes[16:]
result_no_header = bytearray()
for i, byte in enumerate(payload_no_header):
    result_no_header.append(byte ^ key_b[i % len(key_b)])

magic_no_header = int.from_bytes(result_no_header[:4], 'big')
print(f"XOR key_b (skip 16 bytes) magic: 0x{magic_no_header:08x}")

# Spróbuj bez headera z key_c
result_no_header_c = bytearray()
for i, byte in enumerate(payload_no_header):
    result_no_header_c.append(byte ^ key_c[i % len(key_c)])

magic_no_header_c = int.from_bytes(result_no_header_c[:4], 'big')
print(f"XOR key_c (skip 16 bytes) magic: 0x{magic_no_header_c:08x}")

if magic_no_header_c == 0xCAFEBABE:
    print("✅ Found it! key_c with 16 byte header skip!")
elif magic_no_header == 0xCAFEBABE:
    print("✅ Found it! key_b with 16 byte header skip!")
