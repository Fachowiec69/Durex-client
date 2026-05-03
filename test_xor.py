#!/usr/bin/env python3
import base64

def simple_xor_encode(data, key):
    """Simple XOR encoding with key"""
    key_bytes = key.encode('utf-8')
    result = bytearray()
    
    for i, byte in enumerate(data):
        result.append(byte ^ key_bytes[i % len(key_bytes)])
    
    return bytes(result)

def simple_xor_decode(data, key):
    """Simple XOR decoding with key (same as encoding for XOR)"""
    return simple_xor_encode(data, key)

def main():
    # Test with a simple string
    test_data = b"Hello, World! This is a test."
    key = "DurexKey"
    
    print(f"Original: {test_data}")
    
    # Encode
    encoded = simple_xor_encode(test_data, key)
    print(f"Encoded: {encoded.hex()}")
    
    # Decode
    decoded = simple_xor_decode(encoded, key)
    print(f"Decoded: {decoded}")
    
    # Verify
    if test_data == decoded:
        print("✅ XOR encoding/decoding works correctly!")
    else:
        print("❌ XOR encoding/decoding failed!")
    
    # Test with actual payload
    try:
        with open('payload_raw.txt', 'r') as f:
            base64_data = f.read().strip()
        
        # Decode from base64 to get the actual class bytes
        class_bytes = base64.b64decode(base64_data)
        
        # Encode with XOR
        encoded_bytes = simple_xor_encode(class_bytes, key)
        
        # Decode back
        decoded_bytes = simple_xor_decode(encoded_bytes, key)
        
        if class_bytes == decoded_bytes:
            print("✅ Payload XOR encoding/decoding works correctly!")
            print(f"Original size: {len(class_bytes)} bytes")
            print(f"Encoded size: {len(encoded_bytes)} bytes")
        else:
            print("❌ Payload XOR encoding/decoding failed!")
            
    except Exception as e:
        print(f"Error testing payload: {e}")

if __name__ == "__main__":
    main()