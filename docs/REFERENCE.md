# FastStylus API Reference 🖊️

## Overview
`FastStylus` provides direct native access to digitizers, pens, and styluses on Windows 8/10/11 using the Win32 `WM_POINTER` subsystem. It intercepts sub-millisecond pressure, dual-axis tilt, rotation, and eraser events before standard JVM event queues.

---

## 1. Class: `FastStylus`

### Factory & Lifecycle Methods
* `static FastStylus create(Window window)`: Creates and attaches a native stylus hook to the given AWT/Swing `Window` or `JFrame`.
* `void start()`: Starts the background native polling loop (approx. 120 Hz).
* `void stop()`: Stops the polling thread and releases native window subclass hooks.
* `void addListener(StylusListener listener)`: Registers a callback listener for stylus events.
* `void removeListener(StylusListener listener)`: Removes a registered listener.
* `boolean isStylusAvailable()`: Returns `true` if an active stylus or pen digitizer is detected on the system.
* `int getMaxStylusPoints()`: Returns the maximum number of simultaneous pens supported by the hardware.

---

## 2. Class: `StylusEvent`

Immutable event payload delivered directly to `StylusListener`:

| Field | Type | Description |
|:---|:---:|:---|
| `id` | `int` | Pointer identifier tracking the physical pen contact. |
| `x` | `int` | X coordinate in client window pixels. |
| `y` | `int` | Y coordinate in client window pixels. |
| `pressure` | `int` | Raw digitizer pressure level (`0..1024`). |
| `pressurePercent` | `int` | Normalized pressure percentage (`0..100%`). |
| `tiltX` | `int` | Physical pen tilt angle on X-axis (`-90°..+90°`). |
| `tiltY` | `int` | Physical pen tilt angle on Y-axis (`-90°..+90°`). |
| `rotation` | `int` | Clockwise barrel rotation in degrees (`0..360°`). |
| `width` | `int` | Physical contact bounding width in pixels. |
| `height` | `int` | Physical contact bounding height in pixels. |
| `state` | `State` | State enumeration: `HOVER`, `DOWN`, `MOVE`, `UP`. |
| `isEraser` | `boolean` | `true` if the pen is inverted or eraser tip is active. |
| `isBarrelButton1` | `boolean` | `true` if barrel button 1 is actively pressed. |
| `isBarrelButton2` | `boolean` | `true` if barrel button 2 is actively pressed. |
| `isInverted` | `boolean` | `true` if pen is physically turned upside down. |
| `timestamp` | `long` | Monotonic timestamp in milliseconds. |

---

## 3. Platform Support

| Platform | Architecture | Status | Driver / Subsystem |
|:---|:---:|:---:|:---|
| **Windows 10 / 11** | x64 | ✅ Fully Supported | Native Win32 `WM_POINTER` Digitizer Pipeline |
| **Linux** | x64 / AArch64 | 🚧 Planned | `libinput` / `evdev` Stylus Tablet Tool API |
| **macOS** | Apple Silicon / x64 | 🚧 Planned | `NSEventSubtypeTabletPoint` & CoreGraphics |

---
**Part of the FastJava Ecosystem** — *Making the JVM faster.* 🚀