# FastStylus v0.1.0 [ALPHA] — Native Stylus/Pen Input for Java

[![Status](https://img.shields.io/badge/status-v0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastStylus/releases/tag/v0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe)

**⚡ Ultra-fast native stylus input for Java — Pressure, tilt, eraser, and hover impossible in pure Java. Native stylus/pen input via Windows WM_POINTER API**.

FastStylus provides **hardware-level stylus access** for Java applications — something impossible with standard
AWT/Swing. Get raw pen data including:

- **Pressure sensitivity** — 0-1024 levels (0-100% mapped)
- **Tilt X/Y** — Pen angle in degrees (-90° to +90°)
- **Rotation/Orientation** — 0-360°
- **Eraser detection** — Automatic eraser tip recognition
- **Barrel buttons** — Two side button support
- **Hover** — Proximity detection without contact
- **Low latency** — Native Windows API, no JVM event queue delays

**Java CANNOT do this.** AWT only provides mouse emulation for pen input. FastStylus gives you the real thing — perfect
for Surface Pro, Wacom, and other Windows Ink devices.

[![FastKeyboard Showcase](docs/screenshot.png)](https://www.youtube.com/watch?v=BZsqQl7WqWk)

---

## Table of Contents

- [TODO](#features)

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

## Why FastStylus?

| Feature          | Java AWT/Swing           | FastStylus (JNI)         |
|------------------|--------------------------|--------------------------|
| Pressure         | ❌ No                     | ✅ 0-1024 levels (0-100%) |
| Tilt X/Y         | ❌ No                     | ✅ -90° to +90°           |
| Rotation         | ❌ No                     | ✅ 0-360°                 |
| Eraser Detection | ❌ No                     | ✅ Automatic              |
| Barrel Buttons   | ❌ No                     | ✅ 2 buttons              |
| Hover            | ❌ No                     | ✅ Proximity detection    |
| Raw Pen Events   | ❌ No (synthesized mouse) | ✅ Native WM_POINTER      |
| Latency          | High (event queue)       | **Native speed**         |

---


## Installation

### Maven (JitPack)

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>com.github.andrestubbe</groupId>
    <artifactId>faststylus</artifactId>
    <version>v0.1.0</version>
  </dependency>
  <dependency>
    <groupId>com.github.andrestubbe</groupId>
    <artifactId>fastcore</artifactId>
    <version>v0.1.0</version>
  </dependency>
</dependencies>

```

### Gradle (JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.andrestubbe:faststylus:1.0.0'
    implementation 'com.github.andrestubbe:fastcore:v0.1.0'

}
```

### Direct Download

- [faststylus-v1.0.0.jar](https://github.com/andrestubbe/FastStylus/releases/download/v1.0.0/faststylus-v1.0.0.jar) —
  Main library with DLL
- [fastcore-v1.0.0.jar](https://github.com/andrestubbe/FastCore/releases/download/v1.0.0/fastcore-1.0.0.jar) — JNI
  loader (required dependency)


---

## API Reference

### Core Methods

| Method                      | Description                  | Status    |
|-----------------------------|------------------------------|-----------|
| `FastStylus.create(window)` | Initialize stylus for window | ✅ Working |
| `addListener(listener)`     | Add stylus event callback    | ✅ Working |
| `start()`                   | Begin stylus polling         | ✅ Working |
| `stop()`                    | Stop stylus polling          | ✅ Working |
| `isStylusAvailable()`       | Check if stylus present      | ✅ Working |
| `getMaxStylusPoints()`      | Get max simultaneous pens    | ✅ Working |

### StylusEvent Fields

| Field             | Type    | Description                 |
|-------------------|---------|-----------------------------|
| `id`              | int     | Pointer ID (tracking)       |
| `x, y`            | int     | Screen coordinates          |
| `pressure`        | int     | 0-1024 raw pressure         |
| `pressurePercent` | int     | 0-100% mapped pressure      |
| `tiltX`           | int     | X tilt angle (-90° to +90°) |
| `tiltY`           | int     | Y tilt angle (-90° to +90°) |
| `rotation`        | int     | Rotation 0-360°             |
| `width, height`   | int     | Contact size in pixels      |
| `state`           | State   | HOVER / DOWN / MOVE / UP    |
| `isEraser`        | boolean | Eraser tip active           |
| `isBarrelButton1` | boolean | Barrel button 1 pressed     |
| `isBarrelButton2` | boolean | Barrel button 2 pressed     |
| `isInverted`      | boolean | Pen inverted (eraser end)   |
| `timestamp`       | long    | Event time in ms            |

---

## Build from Source

See [COMPILE.md](docs/COMPILE.md) for detailed build instructions.

---

## 📄 License

MIT License — See [LICENSE](LICENSE) for details.

---

## Project Structure

```
faststylus/
├── .github/workflows/          # CI/CD
├── examples/00-basic-usage/   # Demo project
│   ├── pom.xml
│   └── src/main/java/faststylus/StylusDemo.java
├── native/
│   ├── FastStylus.cpp         # Native implementation
│   └── FastStylus.def         # JNI exports (REQUIRED)
├── src/main/java/faststylus/  # Library source
│   └── FastStylus.java
├── compile.bat                # Native build script
├── pom.xml                    # Maven config
└── README.md                  # This file
```

---

## 🖊️ Compatible Devices

- **Microsoft Surface** Pro 8/9, Studio, Go, Laptop (with Surface Pen)
- **Wacom** Penabled, AES, and EMR devices
- **Windows Ink** compatible pens
- **HP, Dell, Lenovo** 2-in-1 devices with active pens

### ✅ Tested Devices

| Device        | Pen                       | Pressure | Tilt   | Eraser | Barrel      | Status                  |
|---------------|---------------------------|----------|--------|--------|-------------|-------------------------|
| Surface Pro 8 | **Wacom Bamboo Ink Plus** | ✅ 0-1024 | ✅ ±90° | ✅      | ✅ 2 buttons | ✅ **Verified Apr 2026** |

> **We need your help!** If you test FastStylus with your device,
> please [open an issue](https://github.com/andrestubbe/FastStylus/issues) with your results and we'll add it to the
> compatibility list.

### 🙋 Call for Volunteers

FastStylus needs testing on more hardware! If you have:

- A Windows tablet/laptop with stylus support
- Any active pen (Wacom, Surface, HP, Dell, Lenovo, etc.)
- 5 minutes to run the demo

Please test and report:

1. Does pressure work? (0-100% in HUD)
2. Does tilt work? (X/Y degrees when angling pen)
3. Does eraser work? (flip pen, HUD shows "ERASER")
4. Do barrel buttons work? (HUD shows "BTN1/BTN2")

[Submit your test results →](https://github.com/andrestubbe/FastStylus/issues/new)

---

## License

MIT License — See [LICENSE](LICENSE) file for details.

---

## Related Projects
- [FastCore](https://github.com/andrestubbe/FastCore) — Native Library Loader & JNI Utilities for Java
- [FastMouse](https://github.com/andrestubbe/FastMouse) — High-Performance Native Mouse API for Java
- [FastHotkey](https://github.com/andrestubbe/FastHotkey) — Low-Latency Global Hotkey API for Java
- [FastKeyboard](https://github.com/andrestubbe/FastKeyboard) — Native Windows RawInput API for Java
- [FastKeylogger](https://github.com/andrestubbe/FastKeylogger) — Behavioral Typing Logic for Java
- [FastTouch](https://github.com/andrestubbe/FastTouch) — Native touchscreen input for Java

---
**Part of the FastJava Ecosystem** — *Making the JVM faster.*




