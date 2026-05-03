#!/usr/bin/env python3
import base64

# Stwórz bardzo prosty test payload
test_data = b"Hello World Test"
key = "DurexKey"

# XOR encode
key_bytes = key.encode('utf-8')
encoded = bytearray()
for i, byte in enumerate(test_data):
    encoded.append(byte ^ key_bytes[i % len(key_bytes)])

# Convert to base64
encoded_b64 = base64.b64encode(bytes(encoded)).decode('utf-8')
print(f"Test payload: {encoded_b64}")

# Test decode
decoded_bytes = base64.b64decode(encoded_b64)
result = bytearray()
for i, byte in enumerate(decoded_bytes):
    result.append(byte ^ key_bytes[i % len(key_bytes)])

print(f"Decoded: {bytes(result)}")