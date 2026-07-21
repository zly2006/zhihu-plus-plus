# =============================================================================
# markdown-renderer consumer rules
#
# This file is packaged into the Android AAR and merged into the consuming app's
# R8/ProGuard configuration.
#
# Rule policy:
# - Keep only metadata attributes that downstream Kotlin reflection may rely on.
# - Do not add package-wide -keep rules for ordinary public APIs.
# - Do not add global switches such as -dontobfuscate or -ignorewarnings.
# =============================================================================

# markdown-renderer exposes Compose-based rendering APIs. Public entry points are
# invoked directly by callers and do not require reflection-based class lookup,
# JNI entry points, or ServiceLoader discovery inside this module.
#
# Transitive libraries such as Coil or other third-party dependencies should
# provide their own consumer rules when needed. This module only preserves
# Kotlin-relevant metadata attributes for downstream reflection compatibility.
-keepattributes InnerClasses,EnclosingMethod,Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
