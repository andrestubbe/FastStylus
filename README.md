# FastStylus 0.1.0 [ALPHA-2026-05-23] — Ultra-Fast Native Windows Stylus & Pen Input Engine for Java

[![Status](https://img.shields.io/badge/status-0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastStylus/releases/tag/0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-0.1.0-green.svg)](https://jitpack.io/#andrestubbe/FastStylus)

---

**⚡ High-speed Win32 WM_POINTER stylus digitizer interception, pressure tracking, tilt angles, and eraser detection for Java.**

**FastStylus** provides hardware-level stylus and pen access directly from the Win32 Pointer API (`WM_POINTER`), bypassing the single-cursor mouse emulation limitations of standard AWT and Swing. Capture 10-bit raw pressure (`0..1024`), dual-axis tilt angles (`-90°..+90°`), continuous rotation, barrel buttons, and hardware eraser state with minimal latency.

[![FastStylus Showcase](docs/screenshot.png)](https://github.com/andrestubbe/FastStylus/releases/tag/0.1.0)

---

## Quick Start

```java
import faststylus.FastStylus;
import javax.swing.JFrame;

public class Demo {
    public static void main(String[] args) {
        JFrame frame = new JFrame("FastStylus Demo");
        frame.setSize(1280, 800);
        frame.setVisible(true);

        // 1. Initialize native stylus interception on the target window
        FastStylus stylus = FastStylus.create(frame);

        // 2. Add real-time stylus listener
        stylus.addListener(event -> {
            System.out.printf("[STYLUS] ID=%d Pos=(%d,%d) Pressure=%d%% Tilt=(%d,%d) Eraser=%s Phase=%s\n",
                event.id, event.x, event.y, event.pressurePercent, event.tiltX, event.tiltY, event.isEraser, event.state);
        });

        // 3. Start background polling thread (~120 Hz)
        stylus.start();
    }
}
```

---

## Table of Contents

- [Quick Start](#quick-start)
- [Why FastTouch?](#why-fasttouch)
- [Key Features](#key-features)
- [Real-World Use Cases](#real-world-use-cases)
- [Performance Benchmarks](#performance-benchmarks)
- [API Quick Reference](#api-quick-reference)
- [Technical Demos & Benchmarks](#technical-demos--benchmarks)
- [Installation](#installation)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [Related Projects](#related-projects)
- [License](#license)

---

## Why FastStylus?

Standard Java input subsystems (AWT `MouseListener`, JavaFX, Swing) treat digitizers and pens as generic single-cursor mouse emulations:

- **Single-Cursor Emulation**: Standard AWT discards pen pressure, tilt angles, and barrel button modifiers.
- **Missing Sensor Geometry**: Physical force levels (`0..1024`), dual-axis tilt (`-90°..+90°`), and barrel rotation (`0..360°`) are completely lost in pure Java.
- **Event Queue Delays**: Synthesized mouse events pass through the Event Dispatch Thread (EDT), creating noticeable stroke lag during drawing or handwriting.

**FastStylus** bridges directly to the Win32 `WM_POINTER` digitizer subsystem:

- **Hardware Pressure & Tilt**: Full 0–1024 raw pressure levels (mapped to 0–100%) and continuous dual-axis tilt tracking.
- **Eraser & Invert Sensing**: Automatic detection when the stylus is physically flipped to the eraser end.
- **Sub-Millisecond Event Loop**: Direct window subclassing captures digitizer messages before JVM event queue scheduling.

---

## Key Features

- 🖊️ **Hardware Pressure Tracking** — Full 10-bit raw resolution (0–1024 levels, normalized 0–100%).
- 📐 **Dual-Axis Tilt & Rotation** — Precise X/Y tilt angles (-90° to +90°) and continuous 360° barrel rotation.
- 🔄 **Automatic Eraser & Invert Detection** — Native recognition of inverted pens and hardware eraser tips.
- 🔘 **Dual Barrel Buttons** — Instant detection of barrel side switches (`BTN1`, `BTN2`).
- 🛸 **Proximity Hover Sensing** — Track cursor coordinates while the pen hovers above the screen without contact.
- ⚡ **Zero-Copy JNI Architecture** — Continuous event dispatch with zero garbage collection churn.

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

## Performance Benchmarks

FastStylus is measured using **JMH (Java Microbenchmark Harness)** to ensure zero-overhead event processing:

| Benchmark / Operation | Score (ops/ms) | Ops per Second |
|---|---|---|
| **`benchmarkStylusEventAllocation`** | **~19,400 ops/ms** | **> 19.4 Million** |
| **`benchmarkStylusEventFormatting`** | **~1,520 ops/ms** | **> 1.52 Million** |
| **Native Polling Loop Rate** | **~120 Hz** | **Smooth Real-time Tracking** |

*Measured on Windows 11 (x64), JDK 21+.*

---

## API Quick Reference

| Method | Return Type | Description | Docs |
|---|---|---|---|
| `FastStylus.create(frame)` | `FastStylus` | Resolves window `HWND` and installs native `WM_POINTER` subclass hook. | [Reference](docs/REFERENCE.md#factory--lifecycle-methods) |
| `addListener(listener)` | `void` | Registers a callback for real-time stylus event dispatch. | [Reference](docs/REFERENCE.md#factory--lifecycle-methods) |
| `removeListener(listener)` | `void` | Unregisters a previously registered stylus listener. | [Reference](docs/REFERENCE.md#factory--lifecycle-methods) |
| `start()` | `void` | Launches the dedicated background stylus polling thread (~120 Hz). | [Reference](docs/REFERENCE.md#factory--lifecycle-methods) |
| `stop()` | `void` | Halts the background stylus polling loop. | [Reference](docs/REFERENCE.md#factory--lifecycle-methods) |
| `FastStylus.isStylusAvailable()` | `boolean` | Queries if a physical stylus or active digitizer is present. | [Reference](docs/REFERENCE.md#factory--lifecycle-methods) |
| `FastStylus.getMaxStylusPoints()` | `int` | Returns maximum simultaneous pens supported by hardware. | [Reference](docs/REFERENCE.md#factory--lifecycle-methods) |

---

## Technical Demos & Benchmarks

| Case | Java Example | Launcher | Description |
|---|---|---|---|
| **Interactive Pen HUD & Canvas** | [StylusDemo.java](examples/Demo/src/main/java/faststylus/StylusDemo.java) | `run-demo.bat` | Real-time drawing canvas with pressure-width scaling, tilt circle HUD, and eraser mode. |
| **JMH Microbenchmark Suite** | [Benchmark.java](examples/Benchmark/src/main/java/faststylus/benchmark/Benchmark.java) | `run-benchmark.bat` | Microbenchmark suite profiling stylus event allocation and formatting throughput. |

---

## Installation

### Option 1: Maven (Recommended)

Add the JitPack repository and the dependency to your `pom.xml`:

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
    <!-- Required Native JNI loader -->
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

1. 📦 **[FastStylus-0.1.0.jar](https://github.com/andrestubbe/FastStylus/releases/tag/0.1.0)** (The Core Library with embedded native DLL)
2. ⚙️ **[fastcore-0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/download/0.1.0/fastcore-0.1.0.jar)** (The Mandatory Native Loader)

> [!IMPORTANT]
> All JARs must be in your classpath for the JNI calls to function correctly.

---

## Documentation

- **[COMPILE.md](docs/COMPILE.md)**: Full compilation guide (MSVC C++17 build chain + JNI Setup).
- **[REFERENCE.md](docs/REFERENCE.md)**: Comprehensive API specification, event fields, and hook lifecycle.
- **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: The engineering rationale for hardware-native pen interception.
- **[ROADMAP.md](docs/ROADMAP.md)**: Planned milestone features and performance extensions.
- **[CHANGELOG.md](docs/CHANGELOG.md)**: Complete version history and release notes.

---

## Platform Support

| Platform | Architecture | Status | Driver / Subsystem |
|:---|:---:|:---:|:---|
| **Windows 10 / 11** | x64 | ✅ Fully Supported | Native Win32 `WM_POINTER` Subsystem |
| **Linux** | x64 / AArch64 | 🚧 Planned | `libinput` / `evdev` Stylus Tablet Tool Slots |
| **macOS** | Apple Silicon / x64 | 🚧 Planned | `NSEvent` & TabletProximity / CoreGraphics |

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
