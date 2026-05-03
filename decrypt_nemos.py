import hashlib
from Crypto.Cipher import AES

# ── Rekonstrukcja danych z bytecode ──────────────────────────────────────────

# _PNGQ = [39, 327, 615, 871, 999, 231, 935]
# q(i) = _PNGQ[i] XOR 77 XOR 41 - 3
PNGQ_raw = [39, 327, 615, 871, 999, 231, 935]
def q(i): return PNGQ_raw[i] ^ 77 ^ 41 - 3  # XOR 77, XOR 41, sub 3

# Sprawdź: q(i) = _PNGQ[i] ^ 77 ^ 41 - 3
# Bytecode: getstatic _PNGQ, iaload, bipush 77, ixor, bipush 41, ixor, iconst_3, isub
def q_correct(i):
    v = PNGQ_raw[i]
    v = v ^ 77
    v = v ^ 41
    v = v - 3
    return v

print("q values:", [q_correct(i) for i in range(7)])

# spanK() = 32, spanIv() = 16

SPAN_K = 32
SPAN_IV = 16

# ── Bufory klucza (z bytecode statycznych inicjalizatorów) ───────────────────

# RenderManager.VBO_INDICES (criticalUvScratch zwraca kopię)
render = bytes([122, 61, 256-69, 93, 256-107, 61, 6, 24, 256-3, 256-57, 256-108, 48, 112, 64, 256-77, 256-47, 82, 256-33, 256-63])

# TestClasses.KEY_STATE_MAP (hardwareNoiseSample zwraca kopię)
# bajt 10 = iconst_5 = 5 (nie bipush)
test = bytes([256-31, 14, 256-16, 256-65, 256-32, 256-72, 62, 96, 113, 256-107, 5, 113, 109, 73, 256-115, 22, 256-41, 256-57, 256-106])

# TemplateModClient.FALLBACK_PALETTE (defaultLutFallback zwraca kopię)
# pełne 19 bajtów z bytecode
tmpl = bytes([256-64, 59, 96, 80, 256-6, 256-46, 14, 256-56, 256-105, 256-29, 96, 256-100, 256-70, 126, 256-72, 256-2, 256-103, 256-114, 256-3])

# ── weaveBuffers: interleave round-robin (0->a, 1->b, 2->c) ─────────────────
def weaveBuffers(a, b, c):
    total = len(a) + len(b) + len(c)
    result = bytearray(total)
    ia = ib = ic = 0
    for i in range(total):
        m = i % 3
        if m == 0:
            result[i] = a[ia]; ia += 1
        elif m == 1:
            result[i] = b[ib]; ib += 1
        else:
            result[i] = c[ic]; ic += 1
    return bytes(result)

woven = weaveBuffers(render, test, tmpl)
print(f"woven ({len(woven)} bytes): {woven.hex()}")

# SHA-256 z woven = klucz AES
aes_key = hashlib.sha256(woven).digest()
print(f"AES key ({len(aes_key)} bytes): {aes_key.hex()}")

# ── Wczytaj plik ─────────────────────────────────────────────────────────────
with open('/tmp/nemos_analysis/extracted/assets/template-mod/internal/hud_overlay.bin', 'rb') as f:
    data = f.read()
print(f"File size: {len(data)}")

# ── Wyciągnij IV i ciphertext z pliku ────────────────────────────────────────
# var9  = data[q(3) : q(3)+spanIv()]          -> IV (16 bajtów)
# var10 = data[q(4) : q(4)+spanK()]           -> XOR stream seed
# var11 = data[q(5) : q(5)+spanK()]           -> permutation seed
# var7  = woven (32 bajty po weave 19+19+19=57... ale spanK=32)
# Uwaga: woven ma 57 bajtów, ale spanK=32 - bierzemy pierwsze 32

iv_start = q_correct(3)
iv_end   = iv_start + SPAN_IV
ct_xor_start = q_correct(4)
ct_xor_end   = ct_xor_start + SPAN_K
perm_start   = q_correct(5)
perm_end     = perm_start + SPAN_K

print(f"IV:   data[{iv_start}:{iv_end}]")
print(f"XOR:  data[{ct_xor_start}:{ct_xor_end}]")
print(f"PERM: data[{perm_start}:{perm_end}]")

iv   = data[iv_start:iv_end]
xor_seed = data[ct_xor_start:ct_xor_end]
perm_seed = data[perm_start:perm_end]

print(f"IV: {iv.hex()}")

# ── Permutacja woven przez permFromSeed ──────────────────────────────────────
def permFromSeed(n, seed_bytes):
    md = hashlib.sha256(seed_bytes).digest()
    perm = list(range(n))
    i = n - 1
    while i > 0:
        md = hashlib.sha256(md).digest()
        r = (md[0] | (md[1] << 8) | (md[2] << 16) | (md[3] << 24)) & 0xFFFFFFFF
        j = r % (i + 1)
        perm[i], perm[j] = perm[j], perm[i]
        i -= 1
    return perm

# var7 = woven[:SPAN_K] (pierwsze 32 bajty woven)
var7 = bytearray(woven[:SPAN_K])
n = len(var7)

perm = permFromSeed(n, perm_seed)

# Zastosuj permutację: var15[i] = var7[perm[i]]  (ale bytecode: var14[perm[i]] = i, potem var15[i] = var7[var14[i]])
# Bytecode linie 245-310:
# var14[perm[i]] = i  (odwrotna permutacja)
# var15[i] = var7[var14[i]]
var14 = [0] * n
for i in range(n):
    var14[perm[i]] = i

var15 = bytearray(n)
for i in range(n):
    var15[i] = var7[var14[i]]

# ── xorSha256Stream(var15, xor_seed) ─────────────────────────────────────────
def xorSha256Stream(buf, seed):
    buf = bytearray(buf)
    md = hashlib.sha256(seed).digest()
    i = 0
    while i < len(buf):
        stream = hashlib.sha256(md).digest()
        md = stream
        j = 0
        while j < len(stream) and i < len(buf):
            buf[i] ^= stream[j]
            i += 1
            j += 1
    return bytes(buf)

var15 = xorSha256Stream(var15, xor_seed)

# ── AES-CTR decrypt ───────────────────────────────────────────────────────────
# key = aes_key (32 bajty = AES-256), iv = iv (16 bajtów)
cipher = AES.new(aes_key, AES.MODE_CTR, nonce=b'', initial_value=iv)
plaintext = cipher.decrypt(var15)

print(f"\nPlaintext ({len(plaintext)} bytes): {plaintext.hex()}")
try:
    text = plaintext.decode('utf-8', errors='replace')
    print(f"As text: {repr(text)}")
except Exception as e:
    print(f"Decode error: {e}")
