# ============================================================
# ProGuard - wersja legit (bez NoFriendClipModule)
# ============================================================

# Dołącz główne reguły
-include proguard-rules.pro

# NoFriendClipModule jest już wykluczony z injars w build.gradle
# (filter: '!pl/durex/client/module/NoFriendClipModule.class')
# Więc nie potrzebujemy dodatkowych reguł tutaj
