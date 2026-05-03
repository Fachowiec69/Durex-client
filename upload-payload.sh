#!/bin/bash

# Skrypt do wgrania payload do Cloudflare KV
# Wymaga zainstalowanego wrangler CLI

# Sprawdź czy wrangler jest zainstalowany
if ! command -v wrangler &> /dev/null; then
    echo "Wrangler CLI nie jest zainstalowany!"
    echo "Zainstaluj przez: npm install -g wrangler"
    exit 1
fi

# Wczytaj payload
PAYLOAD=$(cat payload_raw.txt)

# Wgraj do KV (musisz podać ID swojego KV namespace)
# Znajdź ID w Cloudflare Dashboard → Workers & Pages → KV
echo "Wgrywanie payload do KV..."
wrangler kv:key put --binding=RATE_LIMIT "payload_class" "$PAYLOAD"

echo "Gotowe!"
