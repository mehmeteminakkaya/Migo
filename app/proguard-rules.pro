# Migo app-specific R8/ProGuard rules.
#
# AGP's default Android rules (getDefaultProguardFile) are wired in from
# build.gradle.kts. Third-party consumer rules (pdfbox-android ships its own
# proguard.txt for its reflection-based SecurityHandler lookup) are merged in
# automatically from the dependency's AAR — no app-side rule needed for that.
#
# No custom keep rules yet: the app's data model classes are hand-written to
# org.json.JSONObject with string-literal keys (MigoPreferences.kt
# CareerSession.toJson()/JSONObject.toCareerSession()), not reflection-based,
# so R8 renaming is safe there. Add rules here if that changes.
