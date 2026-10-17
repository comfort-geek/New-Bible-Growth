[app]
title = Enduring Word
package.name = enduringword
package.domain = org.enduringword

source.dir = .
source.include_exts = py,png,jpg,kv,atlas,db
source.include_patterns = enduring_word_full.db

version = 1.0
requirements = python3,kivy
orientation = portrait
fullscreen = 0

[buildozer]
log_level = 2
warn_on_root = 1

[app:android]
archs = armeabi-v7a
android.api = 33
android.minapi = 21
android.ndk = 25b
android.build_tools_version = 33.0.2
android.buildtools_version = 33.0.2
android.accept_sdk_license_agreement = True
android.sdk_path = /usr/local/lib/android/sdk
android.ant_path = /usr/bin/ant
android.permissions = INTERNET

[buildozer:state]
cache.buildozer = %(appdir)s/.buildozer