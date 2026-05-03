import hashlib
from Crypto.Cipher import AES

# Dokładna implementacja TextureAtlasSync.pullSharedDescriptors()
# na podstawie zdekompilowanego kodu Java

# _PNGQ = [39, 327, 615, 871, 999, 231, 935]
# q(i) = (_PNGQ[i] ^ 0x4D ^ 0x29) - 3
PNGQ = [39, 327, 615, 871, 999, 231, 935]
def q(i):
    return (PNGQ[i] ^ 0x4D ^ 0x29) - 3

print("q values:", [q(i) for i in range(7)])

SPAN_K = 32
SPAN_IV = 16

# Wczytaj plik
with open('/tmp/nemos_analysis/extracted/assets/template-mod/internal/hud_overlay.bin', 'rb') as f:
    png = f.read()
print(f"File size: {len(png)}")
print(f"Min required size (q(6)): {q(6)}")

# Klucz AES pochodzi z 3 fragmentów PLIKU (nie z klas Java!)
# pre = png[q(0):q(0)+32] + png[q(1):q(1)+32] + png[q(2):q(2)+32]
lk = SPAN_K
pre = png[q(0):q(0)+lk] + png[q(1):q(1)+lk] + png[q(2):q(2)+lk]
aes_key = hashlib.sha256(pre).digest()
print(f"AES key: {aes_key.hex()}")

# IV
iv = png[q(3):q(3)+SPAN_IV]
print(f"IV: {iv.hex()}")

# maskSeed i permSeed
mask_seed = png[q(4):q(4)+lk]
perm_seed = png[q(5):q(5)+lk]

# Bufory z klas Java (merged = weaveBuffers(a, b, c))
# RenderManager.VBO_INDICES
render = bytes([122, 61, 256-69, 93, 256-107, 61, 6, 24, 256-3, 256-57, 256-108, 48, 112, 64, 256-77, 256-47, 82, 256-33, 256-63])
# TestClasses.KEY_STATE_MAP (bajt[10] = iconst_5 = 5)
test = bytes([256-31, 14, 256-16, 256-65, 256-32, 256-72, 62, 96, 113, 256-107, 5, 113, 109, 73, 256-115, 22, 256-41, 256-57, 256-106])
# TemplateModClient.FALLBACK_PALETTE
tmpl = bytes([256-64, 59, 96, 80, 256-6, 256-46, 14, 256-56, 256-105, 256-29, 96, 256-100, 256-70, 126, 256-72, 256-2, 256-103, 256-114, 256-3])

def weaveBuffers(a, b, c):
    n = len(a) + len(b) + len(c)
    out = bytearray(n)
    ia = ib = ic = 0
    for i in range(n):
        m = i % 3
        if m == 0:
            out[i] = a[ia]; ia += 1
        elif m == 1:
            out[i] = b[ib]; ib += 1
        else:
            out[i] = c[ic]; ic += 1
    return bytes(out)

merged = weaveBuffers(render, test, tmpl)
print(f"merged ({len(merged)} bytes): {merged.hex()}")

def permFromSeed(n, seed32):
    md = hashlib.sha256(seed32).digest()
    perm = list(range(n))
    for k in range(n-1, 0, -1):
        md = hashlib.sha256(md).digest()
        r_int = (md[0] | (md[1] << 8) | (md[2] << 16) | (md[3] << 24))
        # floorMod dla signed int
        r = r_int % (k + 1) if r_int >= 0 else ((r_int % (k+1)) + (k+1)) % (k+1)
        # Python int jest signed, ale & 0xFFFFFFFF daje unsigned
        r_unsigned = r_int & 0xFFFFFFFF
        r = r_unsigned % (k + 1)
        perm[k], perm[r] = perm[r], perm[k]
    return perm

def xorSha256Stream(data, seed):
    data = bytearray(data)
    chain = hashlib.sha256(seed).digest()
    pos = 0
    while pos < len(data):
        block = hashlib.sha256(chain).digest()
        chain = block
        for i in range(len(block)):
            if pos >= len(data):
                break
            data[pos] ^= block[i]
            pos += 1
    return bytes(data)

n = len(merged)
perm = permFromSeed(n, perm_seed)

# inv[perm[i]] = i
inv = [0] * n
for i in range(n):
    inv[perm[i]] = i

# ct[i] = merged[inv[i]]
ct = bytearray(n)
for i in range(n):
    ct[i] = merged[inv[i]]

ct = xorSha256Stream(ct, mask_seed)

# AES-CTR decrypt
cipher = AES.new(aes_key, AES.MODE_CTR, nonce=b'', initial_value=iv)
plain = cipher.decrypt(ct)

print(f"\nPlaintext ({len(plain)} bytes): {plain.hex()}")
try:
    text = plain.decode('utf-8', errors='replace')
    print(f"As text: {repr(text)}")
except Exception as e:
    print(f"Decode error: {e}")
