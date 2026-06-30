# PokeMMO Launcher

This program manages:

- Downloads of the PokeMMO game client
- Signature verification for the downloaded files
- Cache management / storage of the program
- Execution of the program

This program is created as a pairing to the PokeMMO Game Client. PokeMMO's Game Client software is provided with the
PokeMMO License. To view this license, visit https://pokemmo.com/tos/

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

----

# Packaging

This repository provides the bootstrap and update functionality for the following distribution-specific packaging repos, as well as serving as the updater for Windows and portable installations:

- **Flatpak**: [PokeMMO/com.pokemmo.PokeMMO](https://github.com/PokeMMO/com.pokemmo.PokeMMO)
- **Snap**: [PokeMMO/pokemmo-snap](https://github.com/PokeMMO/pokemmo-snap)

----

# Build

This program is built with [Gradle](https://gradle.org/)

## UI Backends

This project provides **two UI backends**:

1. **AWT** – Suitable for running on a standard Java Runtime Environment (JRE).
2. **SWT** – Used when building **native images** via GraalVM Native Image. SWT provides a native look and feel and is linked against platform-specific native libraries.

## Building

### AWT (JRE)

The AWT jar bundles all dependencies including the AWT-based UI and runs on any JRE 17+.

```bash
./gradlew jarAWT
```

The output is written to `build/libs/launcher-awt.jar` and can be run with:

```bash
java -jar launcher-awt.jar
```

### SWT (Native Image)

Native images are platform-specific executables built with GraalVM Native Image. Each platform has its own build task that produces a native binary.

| Platform          | Build task                      | Output       |
|-------------------|---------------------------------|--------------|
| Windows x64       | `nativeWindowsX64Compile`       | Native EXE   |
| Windows ARM64     | `nativeWindowsARM64Compile`     | Native EXE   |
| Linux x64         | `nativeLinuxX64Compile`         | Native binary|
| Linux ARM64       | `nativeLinuxARM64Compile`       | Native binary|
| macOS x64 (Intel) | `nativeMacosX64Compile`         | Native binary|
| macOS ARM64 (Apple Silicon) | `nativeMacosARM64Compile` | Native binary|

To build the native image for your current platform, run:

```bash
./gradlew nativeCompileCI
```

This automatically detects the host OS and architecture and triggers the corresponding native compilation task.

To build a native image for a specific platform (requires the corresponding build toolchain):

```bash
./gradlew nativeLinuxX64Compile
```

### Platform-specific notes

- **Windows**: Builds require a Windows SDK (e.g., Visual Studio Build Tools) for native linking.
- **Linux**: Builds require GTK development libraries (`libgtk-3-dev`, etc.) to link against SWT's GTK bindings.
- **macOS**: Builds require Xcode Command Line Tools for native linking.

### SWT Jar (without native image)

It is also possible to build a platform-specific SWT jar that can be run on a JRE:

```bash
./gradlew jarLinuxX64      # or jarWindowsX64, jarMacosARM64, etc.
```

The resulting jar includes SWT native libraries for the targeted platform.