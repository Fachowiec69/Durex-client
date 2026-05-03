#!/usr/bin/env python3
"""
Skrypt do wgrania payload przez Cloudflare KV API.
Potrzebuje: CF_ACCOUNT_ID, CF_API_TOKEN, CF_KV_NAMESPACE_ID
"""
import requests
import os
import sys

# Dane z Cloudflare Dashboard
# Settings -> API Tokens -> Create Token
# KV Namespace ID z Workers -> KV -> RATE_LIMIT namespace

CF_ACCOUNT_ID = os.environ.get('CF_ACCOUNT_ID', '')
CF_API_TOKEN = os.environ.get('CF_API_TOKEN', '')
CF_KV_NAMESPACE_ID = os.environ.get('CF_KV_NAMESPACE_ID', '')

if not all([CF_ACCOUNT_ID, CF_API_TOKEN, CF_KV_NAMESPACE_ID]):
    print("Użycie:")
    print("  CF_ACCOUNT_ID=xxx CF_API_TOKEN=xxx CF_KV_NAMESPACE_ID=xxx python3 upload_via_cf_api.py")
    print("")
    print("Gdzie znaleźć:")
    print("  CF_ACCOUNT_ID: Cloudflare Dashboard -> prawy panel -> Account ID")
    print("  CF_API_TOKEN: My Profile -> API Tokens -> Create Token (Workers KV Storage: Edit)")
    print("  CF_KV_NAMESPACE_ID: Workers & Pages -> KV -> RATE_LIMIT -> ID")
    sys.exit(1)

# Wczytaj payload
with open('payload_raw.txt', 'r') as f:
    payload_data = f.read().strip()

print(f"Payload size: {len(payload_data)} characters")

# Wgraj przez Cloudflare KV API
url = f"https://api.cloudflare.com/client/v4/accounts/{CF_ACCOUNT_ID}/storage/kv/namespaces/{CF_KV_NAMESPACE_ID}/values/payload_class"

response = requests.put(
    url,
    headers={
        'Authorization': f'Bearer {CF_API_TOKEN}',
        'Content-Type': 'text/plain'
    },
    data=payload_data
)

if response.status_code == 200:
    result = response.json()
    if result.get('success'):
        print("✅ Payload wgrany pomyślnie!")
    else:
        print(f"❌ Błąd: {result}")
else:
    print(f"❌ HTTP {response.status_code}: {response.text}")
