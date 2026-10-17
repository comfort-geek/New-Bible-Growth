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
android.accept_sdk_license_agreement = True
android.permissions = INTERNET

[buildozer:state]
cache.buildozer = %(appdir)s/.buildozer