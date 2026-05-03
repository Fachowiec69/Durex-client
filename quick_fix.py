#!/usr/bin/env python3
import base64

# Najprostsze rozwiązanie - wgrajmy payload_raw.txt bezpośrednio jako base64
# bez żadnego kodowania, żeby sprawdzić czy problem jest w kodowaniu czy w czymś innym

with open('payload_raw.txt', 'r') as f:
    raw_payload = f.read().strip()

print(f"Raw payload size: {len(raw_payload)} characters")
print(f"First 100 chars: {raw_payload[:100]}")

# Sprawdźmy czy to jest prawidłowy base64
try:
    decoded = base64.b64decode(raw_payload)
    print(f"Decoded size: {len(decoded)} bytes")
    
    # Sprawdźmy magic number (pierwsze 4 bajty Java class file to 0xCAFEBABE)
    if len(decoded) >= 4:
        magic = int.from_bytes(decoded[:4], 'big')
        print(f"Magic number: 0x{magic:08x}")
        if magic == 0xCAFEBABE:
            print("✅ Valid Java class file!")
        else:
            print("❌ Invalid Java class file magic number")
    
except Exception as e:
    print(f"❌ Invalid base64: {e}")