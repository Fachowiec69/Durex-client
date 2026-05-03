#!/usr/bin/env python3
import base64

def simple_xor_encode(data, key):
    """Simple XOR encoding with key"""
    key_bytes = key.encode('utf-8')
    result = bytearray()
    
    for i, byte in enumerate(data):
        result.append(byte ^ key_bytes[i % len(key_bytes)])
    
    return bytes(result)

def main():
    # Read the raw payload
    try:
        with open('payload_raw.txt', 'r') as f:
            base64_data = f.read().strip()
        
        # Decode from base64 to get the actual class bytes
        class_bytes = base64.b64decode(base64_data)
        print(f"Original class size: {len(class_bytes)} bytes")
        
        # Encode with simple XOR
        key = "DurexKey"
        encoded_bytes = simple_xor_encode(class_bytes, key)
        print(f"Encoded size: {len(encoded_bytes)} bytes")
        
        # Encode back to base64 for storage
        encoded_base64 = base64.b64encode(encoded_bytes).decode('utf-8')
        
        # Save the encoded payload
        with open('payload_simple.b64', 'w') as f:
            f.write(encoded_base64)
        
        print("Simple XOR encoded payload saved to payload_simple.b64")
        print(f"Base64 length: {len(encoded_base64)} characters")
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()