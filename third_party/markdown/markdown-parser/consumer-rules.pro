# =============================================================================
# markdown-parser consumer rules
#
# This file is packaged into the Android AAR and merged into the consuming app's
# R8/ProGuard configuration.
#
# Rule policy:
# - Keep only metadata attributes that downstream Kotlin reflection may rely on.
# - Do not add package-wide -keep rules for ordinary public APIs.
# - Do not add global switches such as -dontobfuscate or -ignorewarnings.
# =============================================================================

# markdown-parser is a pure Kotlin parser library. Its public API is referenced
# directly by callers and does not rely on runtime reflection, JNI, ServiceLoader,
# or runtime serialization registration.
#
# We therefore do not keep classes or members. We only preserve Kotlin-relevant
# metadata attributes so downstream apps using kotlin-reflect on this library's
# public types still see generic signatures, nested type ownership, and runtime
# annotations.
-keepattributes InnerClasses,EnclosingMethod,Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
