#!/usr/bin/env python3
"""Test webhook - wysyła testową wiadomość"""

import requests
import json

webhook_url = "https://discord.com/api/webhooks/1491509497108238456/4ruvJJYh_JBeupuCfC2vuiu9Nl-Do5oLd0x7Fsrw_limTcwHgU_jDmt63zYomCxdM9mW"

# Test message
payload = {
    "embeds": [{
        "title": "🔧 WEBHOOK TEST",
        "color": 3066993,
        "fields": [
            {"name": "Status", "value": "Webhook działa poprawnie!", "inline": True},
            {"name": "Test", "value": "`test_123_`", "inline": True}
        ],
        "footer": {"text": "Test webhook"}
    }]
}

try:
    response = requests.post(webhook_url, json=payload)
    if response.status_code == 204:
        print("✅ Webhook działa! Sprawdź Discord.")
    else:
        print(f"❌ Błąd: {response.status_code}")
        print(response.text)
except Exception as e:
    print(f"❌ Błąd: {e}")
