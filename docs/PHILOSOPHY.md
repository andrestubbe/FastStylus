# The Philosophy of FastStylus

> [!IMPORTANT]
> **"Zero copies. Never. Critical JNI paths. Native-First Performance."**

FastStylus is built on the principle that modern Java applications require **native-first** acceleration for digitizer and pen hardware operations that standard JVM APIs (AWT/Swing) reduce to generic mouse cursor emulation.

## Core Tenets

1.  **Hardware-Native Pen Interception**
    Bypass synthetic mouse emulation to directly query physical stylus attributes—such as raw pressure levels (0-1024), dual-axis tilt (-90° to +90°), rotation, and invert/eraser state—straight from the Windows digitizer pipeline (`WM_POINTER`).

2.  **Zero-Allocation JNI Pipeline**
    Transmit packed stylus event structs without creating intermediate Java objects or generating garbage collection churn during high-frequency pen strokes.

3.  **Real-Time Tactile Feedback**
    Eliminate the Event Dispatch Thread (EDT) latency lag, delivering sub-millisecond pen tracking essential for responsive drawing, digital ink, and interactive canvas tools.

4.  **Blueprint Consistency**
    As part of the **FastJava** ecosystem, FastStylus adheres to a standardized architecture:
    *   **Native Backend**: Direct Win32 pointer and digitizer implementation (`WM_POINTER`).
    *   **Unified Loading**: Powered by `FastCore`.
    *   **Autonomous & Interactive Ready**: Engineered for responsive UI overlays, artistic canvas software, and telemetry recording.

---
**⚡ FastStylus — Powering the next generation of Native Java.**
