/**
 * FastStylus - Native Stylus/Pen Input for Java
 * Windows API Implementation (WM_POINTER - Windows 10/11)
 * Supports Surface Pro 8, Wacom, and Windows Ink devices
 */

#include <jni.h>
#include <windows.h>
#include <stdio.h>

#pragma comment(lib, "user32.lib")

// WM_POINTER constants (Windows 8+)
#ifndef WM_POINTERDOWN
#define WM_POINTERDOWN 0x0246
#define WM_POINTERUP 0x0247
#define WM_POINTERUPDATE 0x0245
#define WM_POINTERENTER 0x0249
#define WM_POINTERLEAVE 0x024A
#define GET_POINTERID_WPARAM(wParam) (LOWORD(wParam))
#endif

// Pointer flags for stylus features (only define if missing)
#ifndef POINTER_FLAG_ERASER
#define POINTER_FLAG_ERASER 0x00004000
#endif
#ifndef POINTER_FLAG_FIRSTBUTTON
#define POINTER_FLAG_FIRSTBUTTON 0x00010000  // Barrel button 1
#endif
#ifndef POINTER_FLAG_SECONDBUTTON
#define POINTER_FLAG_SECONDBUTTON 0x00020000  // Barrel button 2
#endif
#ifndef POINTER_FLAG_THIRDBUTTON
#define POINTER_FLAG_THIRDBUTTON 0x00040000
#endif
#ifndef POINTER_FLAG_FOURTHBUTTON
#define POINTER_FLAG_FOURTHBUTTON 0x00080000
#endif
#ifndef POINTER_FLAG_FIFTHBUTTON
#define POINTER_FLAG_FIFTHBUTTON 0x00100000
#endif
#ifndef POINTER_FLAG_INCONTACT
#define POINTER_FLAG_INCONTACT 0x00000004
#endif
#ifndef POINTER_FLAG_INRANGE
#define POINTER_FLAG_INRANGE 0x00000002
#endif

// Pointer type constants (fallback)
#ifndef PT_POINTER
#define PT_POINTER 0x00000001
#endif
#ifndef PT_TOUCH
#define PT_TOUCH 0x00000002
#endif
#ifndef PT_PEN
#define PT_PEN 0x00000003
#endif
#ifndef PT_MOUSE
#define PT_MOUSE 0x00000004
#endif

#ifndef POINTER_DEVICE_PRODUCT_ID_LEN
#define POINTER_DEVICE_PRODUCT_ID_LEN 0x00000014
#endif

// Function pointers for Pointer API
typedef BOOL (WINAPI *GetPointerTouchInfoFunc)(UINT32 pointerId, POINTER_TOUCH_INFO* touchInfo);
typedef BOOL (WINAPI *GetPointerPenInfoFunc)(UINT32 pointerId, POINTER_PEN_INFO* penInfo);
typedef BOOL (WINAPI *GetPointerInfoFunc)(UINT32 pointerId, POINTER_INFO* pointerInfo);
typedef BOOL (WINAPI *GetPointerFramePenInfoFunc)(UINT32 pointerId, UINT32* pointerCount, POINTER_PEN_INFO* penInfo);
typedef BOOL (WINAPI *EnableMouseInPointerFunc)(BOOL fEnable);

static GetPointerTouchInfoFunc pGetPointerTouchInfo = nullptr;
static GetPointerPenInfoFunc pGetPointerPenInfo = nullptr;
static GetPointerInfoFunc pGetPointerInfo = nullptr;
static GetPointerFramePenInfoFunc pGetPointerFramePenInfo = nullptr;
static EnableMouseInPointerFunc pEnableMouseInPointer = nullptr;

// Global state
static HWND g_hwnd = nullptr;
static bool g_initialized = false;
static bool g_stylusAvailable = false;

// Stylus point storage (ring buffer)
#define MAX_STYLUS_POINTS 10

struct StylusPoint {
    int id;
    int x;
    int y;
    int pressure;       // 0-1024 (Windows API)
    int tiltX;          // -90 to +90 degrees
    int tiltY;          // -90 to +90 degrees
    int rotation;       // 0-359 degrees
    int width;          // Contact width in pixels
    int height;         // Contact height in pixels
    long timestamp;
    int state;          // 0=HOVER, 1=DOWN, 2=MOVE, 3=UP
    bool active;
    bool isEraser;
    bool isBarrelButton1;
    bool isBarrelButton2;
    bool isInverted;
};

static StylusPoint g_stylusPoints[MAX_STYLUS_POINTS];
static int g_stylusCount = 0;
static CRITICAL_SECTION g_stylusLock;

// Window procedure hook
static WNDPROC g_origWndProc = nullptr;

// Helper: Check if a pointer is a stylus/pen (not touch or mouse)
static bool IsStylusPointer(UINT32 pointerId) {
    if (!pGetPointerInfo) return false;
    
    POINTER_INFO ptrInfo;
    if (pGetPointerInfo(pointerId, &ptrInfo)) {
        // Check pointer type - accept PEN, TOUCH (Bamboo Ink reports as touch), or UNKNOWN
        // Bamboo Ink Plus reports as PT_TOUCH but has pen properties via GetPointerPenInfo
        return (ptrInfo.pointerType == PT_PEN || ptrInfo.pointerType == PT_TOUCH || ptrInfo.pointerType == 0);
    }
    return false;
}

// Window procedure to intercept stylus events
static LRESULT CALLBACK StylusWndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    // Debug: Log all pointer messages
    switch (msg) {
        case WM_POINTERDOWN:
        case WM_POINTERUPDATE:
        case WM_POINTERUP:
        case WM_POINTERENTER:
        case WM_POINTERLEAVE: {
            UINT32 pointerId = GET_POINTERID_WPARAM(wParam);
            
            // Only process pen/stylus pointers, ignore touch
            if (!IsStylusPointer(pointerId)) {
                break;  // Let touch/mouse pass through
            }
            
            EnterCriticalSection(&g_stylusLock);
            
            // Find existing stylus slot
            int slot = -1;
            for (int j = 0; j < MAX_STYLUS_POINTS; j++) {
                if (g_stylusPoints[j].id == (int)pointerId && g_stylusPoints[j].active) {
                    slot = j;
                    break;
                }
            }
            
            // Handle UP separately
            if (msg == WM_POINTERUP) {
                if (slot != -1) {
                    g_stylusPoints[slot].state = 3; // UP
                    g_stylusPoints[slot].active = false;
                    g_stylusPoints[slot].pressure = 0;
                    g_stylusPoints[slot].tiltX = 0;
                    g_stylusPoints[slot].tiltY = 0;
                }
                LeaveCriticalSection(&g_stylusLock);
                return 0;
            }
            
            // Get pen info for all other messages
            if (pGetPointerPenInfo) {
                POINTER_PEN_INFO penInfo;
                if (pGetPointerPenInfo(pointerId, &penInfo)) {
                    // Find or create slot
                    if (slot == -1) {
                        for (int j = 0; j < MAX_STYLUS_POINTS; j++) {
                            if (!g_stylusPoints[j].active) {
                                slot = j;
                                g_stylusPoints[j].active = true;
                                g_stylusPoints[j].id = pointerId;
                                break;
                            }
                        }
                    }
                    
                    if (slot != -1) {
                        // Coordinates
                        POINT pt;
                        pt.x = penInfo.pointerInfo.ptPixelLocation.x;
                        pt.y = penInfo.pointerInfo.ptPixelLocation.y;
                        ScreenToClient(g_hwnd, &pt);
                        
                        g_stylusPoints[slot].x = pt.x;
                        g_stylusPoints[slot].y = pt.y;
                        g_stylusPoints[slot].timestamp = GetTickCount();
                        
                        // Pressure (Windows gives 0-1024, we store raw)
                        g_stylusPoints[slot].pressure = penInfo.pressure;
                        
                        // Tilt X and Y (in degrees, -90 to +90)
                        g_stylusPoints[slot].tiltX = penInfo.tiltX;
                        g_stylusPoints[slot].tiltY = penInfo.tiltY;
                        
                        // Rotation/Orientation (in degrees, 0-359)
                        g_stylusPoints[slot].rotation = penInfo.rotation;
                        
                        // Contact size - estimate based on pressure (POINTER_PEN_INFO doesn't have rcContact)
                        // Higher pressure = larger contact area
                        int contactSize = 2 + (penInfo.pressure * 20 / 1024);  // 2-22 pixels
                        g_stylusPoints[slot].width = contactSize;
                        g_stylusPoints[slot].height = contactSize;
                        
                        // Button states
                        g_stylusPoints[slot].isEraser = (penInfo.pointerInfo.pointerFlags & POINTER_FLAG_ERASER) != 0;
                        g_stylusPoints[slot].isBarrelButton1 = (penInfo.pointerInfo.pointerFlags & POINTER_FLAG_FIRSTBUTTON) != 0;
                        g_stylusPoints[slot].isBarrelButton2 = (penInfo.pointerInfo.pointerFlags & POINTER_FLAG_SECONDBUTTON) != 0;
                        g_stylusPoints[slot].isInverted = g_stylusPoints[slot].isEraser;  // Eraser usually means inverted
                        
                        // State determination
                        bool inContact = (penInfo.pointerInfo.pointerFlags & POINTER_FLAG_INCONTACT) != 0;
                        bool inRange = (penInfo.pointerInfo.pointerFlags & POINTER_FLAG_INRANGE) != 0;
                        
                        if (inContact) {
                            if (msg == WM_POINTERDOWN) {
                                g_stylusPoints[slot].state = 1; // DOWN
                            } else {
                                g_stylusPoints[slot].state = 2; // MOVE
                            }
                        } else if (inRange) {
                            // Pen is near but not touching (hover)
                            g_stylusPoints[slot].state = 0; // HOVER
                            g_stylusPoints[slot].pressure = 0;  // No pressure in hover
                        }
                    }
                }
            } else if (msg == WM_POINTERENTER || msg == WM_POINTERDOWN) {
                // Fallback if GetPointerPenInfo not available - try basic pointer info
                if (pGetPointerInfo) {
                    POINTER_INFO ptrInfo;
                    if (pGetPointerInfo(pointerId, &ptrInfo)) {
                        for (int j = 0; j < MAX_STYLUS_POINTS; j++) {
                            if (!g_stylusPoints[j].active) {
                                slot = j;
                                g_stylusPoints[j].active = true;
                                g_stylusPoints[j].id = pointerId;
                                
                                POINT pt;
                                pt.x = ptrInfo.ptPixelLocation.x;
                                pt.y = ptrInfo.ptPixelLocation.y;
                                ScreenToClient(g_hwnd, &pt);
                                
                                g_stylusPoints[j].x = pt.x;
                                g_stylusPoints[j].y = pt.y;
                                g_stylusPoints[j].timestamp = GetTickCount();
                                g_stylusPoints[j].pressure = 512; // Default 50%
                                g_stylusPoints[j].tiltX = 0;
                                g_stylusPoints[j].tiltY = 0;
                                g_stylusPoints[j].rotation = 0;
                                g_stylusPoints[j].width = 1;
                                g_stylusPoints[j].height = 1;
                                g_stylusPoints[j].state = (msg == WM_POINTERENTER) ? 0 : 1; // HOVER or DOWN
                                g_stylusPoints[j].isEraser = (ptrInfo.pointerFlags & POINTER_FLAG_ERASER) != 0;
                                g_stylusPoints[j].isBarrelButton1 = (ptrInfo.pointerFlags & POINTER_FLAG_FIRSTBUTTON) != 0;
                                g_stylusPoints[j].isBarrelButton2 = (ptrInfo.pointerFlags & POINTER_FLAG_SECONDBUTTON) != 0;
                                g_stylusPoints[j].isInverted = g_stylusPoints[j].isEraser;
                                break;
                            }
                        }
                    }
                }
            }
            
            // Count active stylus points
            g_stylusCount = 0;
            for (int i = 0; i < MAX_STYLUS_POINTS; i++) {
                if (g_stylusPoints[i].active || g_stylusPoints[i].state == 3) {
                    g_stylusCount++;
                }
            }
            
            LeaveCriticalSection(&g_stylusLock);
            return 0;  // Consume the event (don't pass to other handlers)
        }
    }
    
    if (g_origWndProc) {
        return CallWindowProc(g_origWndProc, hwnd, msg, wParam, lParam);
    }
    return DefWindowProc(hwnd, msg, wParam, lParam);
}

extern "C" {

JNIEXPORT void JNICALL Java_faststylus_FastStylus_initNative(JNIEnv*, jclass, jlong hwnd) {
    g_hwnd = (HWND)hwnd;
    
    InitializeCriticalSection(&g_stylusLock);
    
    // Load Pointer API functions (Windows 8+)
    HMODULE hUser32 = GetModuleHandleA("user32.dll");
    if (hUser32) {
        pGetPointerTouchInfo = (GetPointerTouchInfoFunc)GetProcAddress(hUser32, "GetPointerTouchInfo");
        pGetPointerPenInfo = (GetPointerPenInfoFunc)GetProcAddress(hUser32, "GetPointerPenInfo");
        pGetPointerInfo = (GetPointerInfoFunc)GetProcAddress(hUser32, "GetPointerInfo");
        pGetPointerFramePenInfo = (GetPointerFramePenInfoFunc)GetProcAddress(hUser32, "GetPointerFramePenInfo");
        pEnableMouseInPointer = (EnableMouseInPointerFunc)GetProcAddress(hUser32, "EnableMouseInPointer");
    }
    
    // Check for pen API availability
    g_stylusAvailable = (pGetPointerPenInfo != nullptr || pGetPointerInfo != nullptr);
    
    if (g_stylusAvailable && g_hwnd) {
        // Enable mouse-in-pointer (allows mouse to work alongside stylus)
        if (pEnableMouseInPointer) {
            pEnableMouseInPointer(TRUE);
        }
        
        // Subclass window to intercept pointer messages
        g_origWndProc = (WNDPROC)SetWindowLongPtr(g_hwnd, GWLP_WNDPROC, (LONG_PTR)StylusWndProc);
        g_initialized = true;
    }
}

JNIEXPORT jlong JNICALL Java_faststylus_FastStylus_findWindow(JNIEnv* env, jclass, jstring title) {
    const char* str = nullptr;
    if (title) str = env->GetStringUTFChars(title, nullptr);
    HWND hwnd = FindWindowA(nullptr, str);
    if (title && str) env->ReleaseStringUTFChars(title, str);
    return (jlong)hwnd;
}

JNIEXPORT void JNICALL Java_faststylus_FastStylus_pollNative(JNIEnv*, jclass) {
    // Process window messages
    MSG msg;
    while (PeekMessage(&msg, nullptr, 0, 0, PM_REMOVE)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }
    
    // Check for stale stylus events (no update for > 1000ms = UP)
    EnterCriticalSection(&g_stylusLock);
    DWORD now = GetTickCount();
    for (int i = 0; i < MAX_STYLUS_POINTS; i++) {
        if (g_stylusPoints[i].active && (now - g_stylusPoints[i].timestamp > 1000)) {
            // No update for 1 second - force UP
            g_stylusPoints[i].state = 3; // UP
            g_stylusPoints[i].active = false;
        }
    }
    LeaveCriticalSection(&g_stylusLock);
}

JNIEXPORT jint JNICALL Java_faststylus_FastStylus_getStylusCount(JNIEnv*, jclass) {
    EnterCriticalSection(&g_stylusLock);
    int count = g_stylusCount;
    LeaveCriticalSection(&g_stylusLock);
    return count;
}

JNIEXPORT jint JNICALL Java_faststylus_FastStylus_getStylusId(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return -1;
    return g_stylusPoints[index].id;
}

JNIEXPORT jint JNICALL Java_faststylus_FastStylus_getStylusX(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return 0;
    return g_stylusPoints[index].x;
}

JNIEXPORT jint JNICALL Java_faststylus_FastStylus_getStylusY(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return 0;
    return g_stylusPoints[index].y;
}

JNIEXPORT jint JNICALL Java_faststylus_FastStylus_getStylusPressure(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return 0;
    return g_stylusPoints[index].pressure;
}

JNIEXPORT jint JNICALL Java_faststylus_FastStylus_getStylusTiltX(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return 0;
    return g_stylusPoints[index].tiltX;
}

JNIEXPORT jint JNICALL Java_faststylus_FastStylus_getStylusTiltY(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return 0;
    return g_stylusPoints[index].tiltY;
}

JNIEXPORT jint JNICALL Java_faststylus_FastStylus_getStylusRotation(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return 0;
    return g_stylusPoints[index].rotation;
}

JNIEXPORT jint JNICALL Java_faststylus_FastStylus_getStylusWidth(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return 0;
    return g_stylusPoints[index].width;
}

JNIEXPORT jint JNICALL Java_faststylus_FastStylus_getStylusHeight(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return 0;
    return g_stylusPoints[index].height;
}

JNIEXPORT jint JNICALL Java_faststylus_FastStylus_getStylusState(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return 3; // UP
    return g_stylusPoints[index].state;
}

JNIEXPORT jlong JNICALL Java_faststylus_FastStylus_getStylusTimestamp(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return 0;
    return g_stylusPoints[index].timestamp;
}

JNIEXPORT jboolean JNICALL Java_faststylus_FastStylus_getStylusIsEraser(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return JNI_FALSE;
    return g_stylusPoints[index].isEraser ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_faststylus_FastStylus_getStylusIsBarrelButton1(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return JNI_FALSE;
    return g_stylusPoints[index].isBarrelButton1 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_faststylus_FastStylus_getStylusIsBarrelButton2(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return JNI_FALSE;
    return g_stylusPoints[index].isBarrelButton2 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_faststylus_FastStylus_getStylusIsInverted(JNIEnv*, jclass, jint index) {
    if (index < 0 || index >= MAX_STYLUS_POINTS) return JNI_FALSE;
    return g_stylusPoints[index].isInverted ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_faststylus_FastStylus_isStylusAvailable(JNIEnv*, jclass) {
    return g_stylusAvailable ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_faststylus_FastStylus_getMaxStylusPoints(JNIEnv*, jclass) {
    // Windows supports up to 256 pointer inputs theoretically
    // Stylus devices typically support 1 pen at a time
    return MAX_STYLUS_POINTS;
}

} // extern "C"
