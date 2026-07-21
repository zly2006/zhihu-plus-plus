# =============================================================================
# markdown-runtime consumer rules
#
# This file is packaged into the Android AAR and merged into the consuming app's
# R8/ProGuard configuration.
#
# Rule policy:
# - Keep only metadata attributes that downstream Kotlin reflection may rely on.
# - Do not add package-wide -keep rules for ordinary public APIs.
# - Do not add global switches such as -dontobfuscate or -ignorewarnings.
# =============================================================================

# markdown-runtime exposes directive pipeline APIs and plugin registration types.
# These entry points are referenced directly by Kotlin/Java code and do not rely
# on runtime reflection, JNI, ServiceLoader, or generated serializer lookup.
#
# We therefore do not keep classes or members. We only preserve Kotlin-relevant
# metadata attributes for downstream reflection compatibility.
-keepattributes InnerClasses,EnclosingMethod,Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
