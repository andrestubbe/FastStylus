# Building FastStylus from Source

## Prerequisites

- **JDK 17+** — [Download](https://adoptium.net/)
- **Maven 3.9+** — [Download](https://maven.apache.org/download.cgi)
- **Visual Studio 2022** — Community/Professional/Enterprise/BuildTools
- **Windows 10/11** with stylus/tablet support for testing

## Quick Build

```bash
# Build native DLL first
compile.bat

# Build JAR
mvn clean package -DskipTests
```

## Build Commands

| Command | Purpose |
|---------|---------|
| `compile.bat` | Build native DLL (Windows) |
| `mvn clean compile` | Compile Java only |
| `mvn clean package` | Build JAR with DLL |
| `mvn clean package -DskipTests` | Fast build |

## Native DLL Build

The `compile.bat` script:
- Auto-detects Visual Studio 2022
- Auto-detects JAVA_HOME
- Uses `native\FastStylus.def` for JNI exports
- Outputs to `build\faststylus.dll`

## Running Examples

```bash
cd examples/00-basic-usage
mvn compile exec:java
```

## Troubleshooting

**"Cannot find DLL"** — Run `compile.bat` first

**"UnsatisfiedLinkError"** — Check:
1. DLL built successfully (`build\faststylus.dll` exists)
2. DLL included in JAR (check `pom.xml` resources)
3. JNI exports defined in `native\FastStylus.def`

**"Java version mismatch"** — Ensure JDK 17+ and JAVA_HOME set

**"Stylus not detected"** — Ensure:
1. You have a Windows Ink compatible stylus
2. Windows Ink is enabled in Settings
3. You're testing on a supported device (Surface, Wacom, etc.)
