# FastStylus — Native Stylus/Pen Input for Java

**⚡ Ultra-fast native stylus input for Java — Pressure, tilt, eraser, and hover impossible in pure Java**

[![Release](https://img.shields.io/badge/release-v1.0.0-blue.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-available-brightgreen.svg)](https://jitpack.io/#andrestubbe/FastStylus)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> **Native stylus/pen input** via Windows WM_POINTER API. Powered by FastCore.

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

## 📦 Why FastStylus?

| Feature | Java AWT/Swing | FastStylus (JNI) |
|---------|---------------|-----------------|
| Pressure | ❌ No | ✅ 0-1024 levels (0-100%) |
| Tilt X/Y | ❌ No | ✅ -90° to +90° |
| Rotation | ❌ No | ✅ 0-360° |
| Eraser Detection | ❌ No | ✅ Automatic |
| Barrel Buttons | ❌ No | ✅ 2 buttons |
| Hover | ❌ No | ✅ Proximity detection |
| Raw Pen Events | ❌ No (synthesized mouse) | ✅ Native WM_POINTER |
| Latency | High (event queue) | **Native speed** |

---

## 🚀 Quick Start

```java
import faststylus.FastStylus;
import faststylus.FastStylus.StylusEvent;

import javax.swing.JFrame;

public class StylusDemo {
    public static void main(String[] args) {
        JFrame frame = new JFrame("FastStylus Demo");
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
        
        // Your app runs here...
    }
}
```

---

## 📦 Installation

### Maven (JitPack)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.andrestubbe</groupId>
    <artifactId>faststylus</artifactId>
    <version>v1.0.0</version>
</dependency>
```

FastCore is automatically included as a transitive dependency.

### Gradle (JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.andrestubbe:faststylus:v1.0.0'
}
```

### Direct Download

- **faststylus-v1.0.0.jar** — Main library with DLL
- **fastcore-v1.0.0.jar** — [JNI loader](https://github.com/andrestubbe/FastCore/releases)

```bash
java -cp "faststylus-v1.0.0.jar;fastcore-v1.0.0.jar" YourApp
```

### Build from Source

See [COMPILE.md](COMPILE.md) for detailed build instructions.

---

## 🎯 API Reference

### Core Methods

| Method | Description | Status |
|--------|-------------|--------|
| `FastStylus.create(window)` | Initialize stylus for window | ✅ Working |
| `addListener(listener)` | Add stylus event callback | ✅ Working |
| `start()` | Begin stylus polling | ✅ Working |
| `stop()` | Stop stylus polling | ✅ Working |
| `isStylusAvailable()` | Check if stylus present | ✅ Working |
| `getMaxStylusPoints()` | Get max simultaneous pens | ✅ Working |

### StylusEvent Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | int | Pointer ID (tracking) |
| `x, y` | int | Screen coordinates |
| `pressure` | int | 0-1024 raw pressure |
| `pressurePercent` | int | 0-100% mapped pressure |
| `tiltX` | int | X tilt angle (-90° to +90°) |
| `tiltY` | int | Y tilt angle (-90° to +90°) |
| `rotation` | int | Rotation 0-360° |
| `width, height` | int | Contact size in pixels |
| `state` | State | HOVER / DOWN / MOVE / UP |
| `isEraser` | boolean | Eraser tip active |
| `isBarrelButton1` | boolean | Barrel button 1 pressed |
| `isBarrelButton2` | boolean | Barrel button 2 pressed |
| `isInverted` | boolean | Pen inverted (eraser end) |
| `timestamp` | long | Event time in ms |

---

## 🏗️ Build from Source

### Prerequisites
- Windows 10/11 with Stylus/Tablet support
- Java JDK 17+
- Visual Studio 2022 (C++ workload)

### Build
```batch
compile.bat
mvn clean package
```

See [COMPILE.md](COMPILE.md) for detailed instructions.

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
├── COMPILE.md                 # Build instructions
├── pom.xml                    # Maven config
└── README.md                  # This file
```

---

## 🖊️ Compatible Devices

- **Microsoft Surface** Pro 8/9, Studio, Go, Laptop (with Surface Pen)
- **Wacom** Penabled, AES, and EMR devices
- **Windows Ink** compatible pens
- **HP, Dell, Lenovo** 2-in-1 devices with active pens

---

**FastStylus** — *Part of the FastJava Ecosystem*  
- [FastCore](https://github.com/andrestubbe/FastCore) — JNI loader
- [FastTouch](https://github.com/andrestubbe/FastTouch) — Touch input
- More at [github.com/andrestubbe](https://github.com/andrestubbe)
