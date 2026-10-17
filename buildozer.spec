[app]
title = Enduring Word
package.name = enduringword
package.domain = org.enduringword

source.dir = .
source.include_exts = py,png,jpg,kv,atlas,db
source.include_patterns = enduring_word_full.db

version = 1.0
version.regex = __version__ = ['"]([^'"]*)['"]
version.filename = %(source.dir)s/main.py

requirements = python3,kivy
orientation = portrait
fullscreen = 0

[buildozer]
log_level = 2
warn_on_root = 1

[app:android]
# --- FIX FOR YOUR PHONE TECNO arm7a ---
archs = armeabi-v7a
# --- FIX FOR Aidl not found ERROR FROM SCREENSHOT ---
android.api = 33
android.minapi = 21
android.ndk = 25b
android.build_tools_version = 33.0.2
android.buildtools_version = 33.0.2
android.accept_sdk_license_agreement = True
android.ant_path = /usr/bin/ant
android.permissions = INTERNET
android.allow_backup = True
# ---------------------------------------

[buildozer:state]
cache.buildozer = %(appdir)s/.buildozer