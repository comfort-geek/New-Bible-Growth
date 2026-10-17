[app]

# App details
title = Enduring Word
package.name = enduringword
package.domain = org.enduring.word

source.dir = .
source.include_exts = py,png,jpg,kv,atlas,db,sqlite
source.include_patterns = enduring_word_full.db

version = 0.1
requirements = python3,kivy
orientation = portrait

# Android settings - FIXED FOR YOUR LICENSE ERROR
android.api = 33
android.minapi = 21
android.sdk = 33
android.ndk = 25b
android.build_tools_version = 33.0.2
android.accept_sdk_license_agreements = True
android.ant = auto

# Permissions
android.permissions = INTERNET

# (str) Supported orientation (landscape, sensorLandscape, portrait, sensorPortrait or all)
android.orientation = portrait

# (bool) Indicate if the android window is fullscreen
fullscreen = 0

[buildozer]

# (int) Log level (0 = error only, 1 = info, 2 = debug)
log_level = 2

# (str) Path to build artifact storage
warn_on_root = 0

# (str) Supported version for p4a
p4a.branch = master

# (str) URL to download p4a
p4a.url = 

# (str) python-for-android URL
p4a.fork = kivy

# (str) branch/tag for python-for-android
p4a.branch = master

[buildozer:state]

# (str) Cache the state
cache.buildozer = .buildozer/state
