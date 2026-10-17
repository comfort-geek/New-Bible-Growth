[app]
title = Enduring Word
package.name = enduringword
package.domain = com.enduringword
source.dir = .
source.include_exts = py,png,jpg,kv,atlas,db
source.include_patterns = enduring_word_full.db
version = 0.1
requirements = python3,kivy
orientation = portrait
android.api = 33
android.minapi = 21
android.ndk = 25b
android.archs = armeabi-v7a
android.accept_sdk_license_agreements = True
android.permissions = INTERNET
p4a.branch = master

[buildozer]
log_level = 1
warn_on_root = 1