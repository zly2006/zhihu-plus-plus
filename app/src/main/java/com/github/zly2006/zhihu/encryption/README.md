# ZSE Encryption Implementation

## Overview

This directory contains a Kotlin implementation of the ZSE v4 encryption algorithm, which was originally implemented in JavaScript in `app/src/main/assets/zse-v4.js`.

## Background

The original JavaScript implementation is a complex encryption system that includes:

1. **SM4 Block Cipher**: A Chinese national cryptographic standard
2. **Virtual Machine**: The JS code includes a bytecode interpreter that executes encrypted program instructions
3. **XOR-based Encryption**: Data is encrypted using XOR operations with SM4-encrypted keys
4. **Base64 Encoding**: Final output is Base64-encoded

## Implementation Details

### ZseEncryption.kt

The Kotlin implementation in `ZseEncryption.kt` replicates the core encryption algorithm:

- **SM4 S-Box and Constants**: Direct port of the substitution box and round constants
- **SM4 Block Encryption**: 32-round Feistel cipher implementation
- **XOR Encryption**: Matches the `__g.x` function from JavaScript
- **Main encrypt() function**: Matches the `D` function that encodes input and encrypts

### Key Components

1. **SM4_SBOX**: 256-byte substitution box used in SM4 encryption
2. **SM4_CK**: 32 round constants for SM4 encryption rounds
3. **tau()**: S-Box transformation function
4. **sm4EncryptBlock()**: Encrypts a single 16-byte block
5. **xorEncrypt()**: XOR-based encryption using SM4 for key generation
6. **encrypt()**: Main entry point that URL-encodes input and encrypts

## Verification Activity

`ZseVerificationActivity.kt` provides a testing interface to compare the Kotlin and JavaScript implementations:

- Side-by-side result comparison
- Performance measurement
- Developer mode access from Account Settings
- Visual feedback on whether results match

## Usage

To verify the implementation:

1. Enable Developer Mode in Account Settings
2. Click "ZSE加密验证（开发工具）"
3. Enter a test string
4. Click "开始验证" to compare results

## Known Limitations

The JavaScript implementation uses a sophisticated virtual machine that executes bytecode. The Kotlin implementation attempts to replicate the end result but may not achieve bit-perfect compatibility due to:

1. Complex bytecode interpretation in JavaScript
2. Possible differences in URL encoding between environments
3. JavaScript-specific quirks in the original implementation

## Future Work

- [ ] Achieve 100% compatibility with JavaScript results
- [ ] Add more test cases
- [ ] Optimize performance
- [ ] Consider using the bytecode approach if full compatibility is needed
