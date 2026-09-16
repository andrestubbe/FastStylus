# FastStylus 0.1.0 [ALPHA] — Native Stylus/Pen Input for Java

[![Status](https://img.shields.io/badge/status-0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastStylus/releases/tag/0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe/FastStylus)

**⚡ Ultra-fast native stylus input for Java — Pressure, tilt, eraser, and hover impossible in pure Java. Native stylus/pen input via Windows WM_POINTER API**.

---

[![FastStylus Showcase](docs/screenshot.png)](https://github.com/andrestubbe/FastStylus/releases/tag/0.1.0)

---

FastStylus provides **hardware-level stylus access** for Java applications — something impossible with standard AWT/Swing. Get raw pen data including:

- **Pressure sensitivity** — 0-1024 levels (0-100% mapped)
- **Tilt X/Y** — Pen angle in degrees (-90° to +90°)
- **Rotation/Orientation** — 0-360°
- **Eraser detection** — Automatic eraser tip recognition
- **Barrel buttons** — Two side button support
- **Hover** — Proximity detection without contact
- **Low latency** — Native Windows API, no JVM event queue delays

**Java CANNOT do this.** AWT only provides mouse emulation for pen input. FastStylus gives you the real thing — perfect for Surface Pro, Wacom, and other Windows Ink devices.

---

## Quick Start

```java
import faststylus.FastStylus;
import faststylus.FastStylus.StylusEvent;

import javax.swing.JFrame;

public class Example {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Example");
        frame.setSize(800, 600);
        frame.setVisible(true);

        // Initialize native stylus input
        FastStylus stylus = FastStylus.create(frame);

        // Add stylus listener
        stylus.addListener(event -> {
            System.out.println("Stylus " + event.id +
                    " at (" + event.x + "," + event.y + ")" +
                    " pressure=" + event.pressurePercent + "%" +
                    " tilt=(" + event.tiltX + "," + event.tiltY + ")" +
                    " eraser=" + event.isEraser +
                    " state=" + event.state);
        });

        // Start polling
        stylus.start();
    }
}
```

---

## Table of Contents

- [Why FastStylus?](#why-faststylus)
- [Compatible Devices](#-compatible-devices)
- [Installation](#installation)
- [API Reference](#api-reference)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [Related Projects](#related-projects)
- [License](#license)

---

## Why FastStylus?

| Feature | Java AWT/Swing | FastStylus (JNI) |
|---|:---:|:---:|
| **Pressure** | ❌ No | ✅ 0-1024 levels (0-100%) |
| **Tilt X/Y** | ❌ No | ✅ -90° to +90° |
| **Rotation** | ❌ No | ✅ 0-360° |
| **Eraser Detection** | ❌ No | ✅ Automatic |
| **Barrel Buttons** | ❌ No | ✅ 2 buttons |
| **Hover** | ❌ No | ✅ Proximity detection |
| **Raw Pen Events** | ❌ No (synthesized mouse) | ✅ Native `WM_POINTER` |
| **Latency** | High (event queue) | **Native speed** |

---

## 🖊️ Compatible Devices

- **Microsoft Surface** Pro 8/9, Studio, Go, Laptop (with Surface Pen)
- **Wacom** Penabled, AES, and EMR devices
- **Windows Ink** compatible pens
- **HP, Dell, Lenovo** 2-in-1 devices with active pens

### ✅ Tested Devices

| Device | Pen | Pressure | Tilt | Eraser | Barrel | Status |
|---|---|:---:|:---:|:---:|:---:|:---:|
| Surface Pro 8 | **Wacom Bamboo Ink Plus** | ✅ 0-1024 | ✅ ±90° | ✅ | ✅ 2 buttons | ✅ **Verified Apr 2026** |

---

## Installation

### Option 1: Maven (via JitPack)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- FastStylus Library -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastStylus</artifactId>
        <version>0.1.0</version>
    </dependency>
    <!-- Mandatory Native JNI Loader -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastCore</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

### Option 2: Gradle (via JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:FastStylus:0.1.0'
    implementation 'com.github.andrestubbe:FastCore:0.1.0'
}
```

### Option 3: Direct Download (No Build Tool)

Download the latest JARs directly to add them to your classpath:

1. 📦 **[faststylus-0.1.0.jar](https://github.com/andrestubbe/FastStylus/releases/download/0.1.0/faststylus-0.1.0.jar)** (The Core Library with embedded DLL)
2. ⚙️ **[fastcore-0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/download/0.1.0/fastcore-0.1.0.jar)** (The Mandatory Native Loader)

---

## API Reference

### Core Methods

| Method | Description | Status |
|---|---|:---:|
| `FastStylus.create(window)` | Initialize stylus for window | ✅ Working |
| `addListener(listener)` | Add stylus event callback | ✅ Working |
| `start()` | Begin stylus polling | ✅ Working |
| `stop()` | Stop stylus polling | ✅ Working |
| `isStylusAvailable()` | Check if stylus present | ✅ Working |
| `getMaxStylusPoints()` | Get max simultaneous pens | ✅ Working |

### StylusEvent Fields

| Field | Type | Description |
|---|:---:|---|
| `id` | `int` | Pointer ID (tracking) |
| `x, y` | `int` | Screen coordinates in window client space |
| `pressure` | `int` | 0-1024 raw pressure |
| `pressurePercent` | `int` | 0-100% mapped pressure |
| `tiltX` | `int` | X tilt angle (-90° to +90°) |
| `tiltY` | `int` | Y tilt angle (-90° to +90°) |
| `rotation` | `int` | Rotation 0-360° |
| `width, height` | `int` | Contact size in pixels |
| `state` | `State` | HOVER / DOWN / MOVE / UP |
| `isEraser` | `boolean` | Eraser tip active |
| `isBarrelButton1` | `boolean` | Barrel button 1 pressed |
| `isBarrelButton2` | `boolean` | Barrel button 2 pressed |
| `isInverted` | `boolean` | Pen inverted (eraser end) |
| `timestamp` | `long` | Event time in ms |

---

## Documentation

- **[COMPILE.md](docs/COMPILE.md)**: Full compilation guide (MSVC C++17 build chain + JNI Setup).
- **[REFERENCE.md](docs/REFERENCE.md)**: Comprehensive API specification, event fields, and lifecycle contracts.
- **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: The engineering rationale for zero-allocation pen hardware interception.
- **[ROADMAP.md](docs/ROADMAP.md)**: Planned milestone features and performance extensions.
- **[CHANGELOG.md](docs/CHANGELOG.md)**: Complete version history and release notes.

---

## Platform Support

| Platform | Architecture | Status | Driver / Subsystem |
|:---|:---:|:---:|:---|
| **Windows 10 / 11** | x64 | ✅ Fully Supported | Native Win32 `WM_POINTER` Digitizer Pipeline |
| **Linux** | x64 / AArch64 | 🚧 Planned | `libinput` / `evdev` Stylus Tablet Tool API |
| **macOS** | Apple Silicon / x64 | 🚧 Planned | `NSEventSubtypeTabletPoint` & CoreGraphics |

---

## Related Projects

- **[`FastCore`](https://github.com/andrestubbe/FastCore)** — Native Library Loader & JNI Utilities for Java
- **[`FastTouch`](https://github.com/andrestubbe/FastTouch)** — Native Multi-Touch Digitizer API for Java
- **[`FastMouse`](https://github.com/andrestubbe/FastMouse)** — Ultra-Low Latency Native RawInput Mouse Engine
- **[`FastKeyboard`](https://github.com/andrestubbe/FastKeyboard)** — Ultra-Fast Native RawInput Keyboard Engine
- **[`FastHotkey`](https://github.com/andrestubbe/FastHotkey)** — Low-Latency Global Hotkey API for Java
- **[`FastVulkan`](https://github.com/andrestubbe/FastVulkan)** — High-Performance Native Vulkan 2D Rendering Engine

---

## License

MIT License — See [LICENSE](LICENSE) file for details.

---

**Part of the FastJava Ecosystem** — *Making the JVM faster.* 🚀
