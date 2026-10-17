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

[buildozer]
log_level = 2

[app:android]
archs = armeabi-v7a
android.api = 34
android.minapi = 21
android.ndk = 27c
android.accept_sdk_license_agreement = True
android.permissions = INTERNET
p4a.branch = v2023.11.22
android.build_tools_version = 34.0.0
# These 2 lines are now just backup, symlink does main job
android.sdk_path = /usr/local/lib/android/sdk
android.ndk_path = /usr/local/lib/android/sdk/ndk/27.3.13750724

[buildozer:state]
cache.buildozer = %(appdir)s/.buildozer