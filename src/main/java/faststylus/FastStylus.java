package faststylus;

import fastcore.FastCore;
import java.util.ArrayList;
import java.util.List;

/**
 * FastStylus - Native Stylus/Pen Input for Java
 * 
 * Captures stylus events with native Windows Pointer API via JNI.
 * Supports pressure, tilt, eraser, and hover for Surface Pro and Wacom devices.
 */
public class FastStylus {
    
    static {
        FastCore.loadLibrary("faststylus");
    }
    
    /**
     * Single stylus event - immutable data class
     */
    public static final class StylusEvent {
        public final int id;              // Pointer ID
        public final int x;               // X coordinate
        public final int y;               // Y coordinate  
        public final int pressure;        // Pressure (0-1024 from Windows)
        public final int pressurePercent; // Pressure (0-100%)
        public final int tiltX;             // Tilt X angle (-90 to +90 degrees)
        public final int tiltY;             // Tilt Y angle (-90 to +90 degrees)
        public final int rotation;          // Rotation/orientation (0-360 degrees)
        public final int width;             // Contact width in pixels
        public final int height;            // Contact height in pixels
        public final long timestamp;      // Timestamp in ms
        public final State state;           // HOVER, DOWN, MOVE, UP
        public final boolean isEraser;      // True if eraser tip
        public final boolean isBarrelButton1; // Barrel button 1 pressed
        public final boolean isBarrelButton2; // Barrel button 2 pressed
        public final boolean isInverted;    // Pen inverted (eraser end)
        
        public StylusEvent(int id, int x, int y, int pressure, int tiltX, int tiltY, int rotation,
                          int width, int height, long timestamp, State state,
                          boolean isEraser, boolean isBarrelButton1, boolean isBarrelButton2,
                          boolean isInverted) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.pressure = pressure;
            this.pressurePercent = (pressure * 100) / 1024;
            this.tiltX = tiltX;
            this.tiltY = tiltY;
            this.rotation = rotation;
            this.width = width;
            this.height = height;
            this.timestamp = timestamp;
            this.state = state;
            this.isEraser = isEraser;
            this.isBarrelButton1 = isBarrelButton1;
            this.isBarrelButton2 = isBarrelButton2;
            this.isInverted = isInverted;
        }
        
        @Override
        public String toString() {
            return String.format("StylusEvent[id=%d, pos=(%d,%d), pressure=%d%% (raw=%d), tilt=(%d,%d), rotation=%d, size=%dx%d, %s, eraser=%s, barrel1=%s, barrel2=%s]", 
                id, x, y, pressurePercent, pressure, tiltX, tiltY, rotation, width, height, state, 
                isEraser, isBarrelButton1, isBarrelButton2);
        }
    }
    
    public enum State { HOVER, DOWN, MOVE, UP }
    
    // Native Methods
    private static native void initNative(long hwnd);
    private static native void pollNative();
    private static native int getStylusCount();
    private static native int getStylusId(int index);
    
    private static native int getStylusX(int index);
    private static native int getStylusY(int index);
    private static native int getStylusPressure(int index);
    private static native int getStylusTiltX(int index);
    private static native int getStylusTiltY(int index);
    private static native int getStylusRotation(int index);
    private static native int getStylusWidth(int index);
    private static native int getStylusHeight(int index);
    private static native int getStylusState(int index);
    private static native long getStylusTimestamp(int index);
    private static native boolean getStylusIsEraser(int index);
    private static native boolean getStylusIsBarrelButton1(int index);
    private static native boolean getStylusIsBarrelButton2(int index);
    private static native boolean getStylusIsInverted(int index);
    
    private long hwnd;
    private static final List<StylusListener> listeners = new ArrayList<>();
    private volatile boolean running = false;
    private final java.util.Set<Integer> firedUpEvents = new java.util.HashSet<>();
    
    /**
     * Interface for stylus event listener
     */
    public interface StylusListener {
        void onStylusEvent(StylusEvent event);
    }
    
    /**
     * Create FastStylus for a JFrame/Window
     */
    public static FastStylus create(javax.swing.JFrame frame) {
        // Wait until window is visible and title is set
        int waitRetries = 0;
        while ((!frame.isVisible() || frame.getTitle() == null || frame.getTitle().isEmpty()) && waitRetries < 50) {
            try { Thread.sleep(10); } catch (InterruptedException e) {}
            waitRetries++;
        }
        
        String title = frame.getTitle();
        
        // Try different title variants
        long hwnd = 0;
        String[] titleVariants = {
            title,
            title != null ? title.trim() : null,
            "FastStylus Demo"
        };
        
        int retries = 0;
        while (hwnd == 0 && retries < 30) {
            for (String variant : titleVariants) {
                if (variant != null && !variant.isEmpty()) {
                    hwnd = findWindow(variant);
                    if (hwnd != 0) break;
                }
            }
            if (hwnd == 0) {
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                retries++;
            }
        }
        
        if (hwnd == 0) {
            throw new RuntimeException("[FastStylus] Window not found. Title was: '" + title + "'");
        }
        
        return new FastStylus(hwnd);
    }
    
    private static native long findWindow(String title);
    
    private FastStylus(long hwnd) {
        this.hwnd = hwnd;
        initNative(hwnd);
    }
    
    /**
     * Add a stylus listener
     */
    public void addListener(StylusListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a stylus listener
     */
    public void removeListener(StylusListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Start the stylus polling thread
     */
    public void start() {
        if (running) return;
        running = true;
        
        Thread pollThread = new Thread(() -> {
            while (running) {
                poll();
                try {
                    Thread.sleep(8); // ~120Hz polling
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "FastStylus-Poll");
        pollThread.setDaemon(true);
        pollThread.start();
    }
    
    /**
     * Stop the stylus polling thread
     */
    public void stop() {
        running = false;
    }
    
    /**
     * Single poll (blocking)
     */
    public void poll() {
        pollNative();
        
        int count = getStylusCount();
        for (int i = 0; i < count; i++) {
            int id = getStylusId(i);
            int x = getStylusX(i);
            int y = getStylusY(i);
            int pressure = getStylusPressure(i);
            int tiltX = getStylusTiltX(i);
            int tiltY = getStylusTiltY(i);
            int rotation = getStylusRotation(i);
            int width = getStylusWidth(i);
            int height = getStylusHeight(i);
            int stateCode = getStylusState(i);
            long timestamp = getStylusTimestamp(i);
            boolean isEraser = getStylusIsEraser(i);
            boolean isBarrelButton1 = getStylusIsBarrelButton1(i);
            boolean isBarrelButton2 = getStylusIsBarrelButton2(i);
            boolean isInverted = getStylusIsInverted(i);
            
            State state = State.values()[stateCode];
            
            // UP events only fire once
            if (state == State.UP) {
                if (firedUpEvents.contains(id)) {
                    continue;  // Already fired, skip
                }
                firedUpEvents.add(id);  // Mark as fired
            } else if (state == State.DOWN || state == State.HOVER) {
                firedUpEvents.remove(id);  // Reset UP tracking
            } else {
                // On MOVE: reset UP tracking for this ID if needed
                firedUpEvents.remove(id);
            }
            
            StylusEvent event = new StylusEvent(id, x, y, pressure, tiltX, tiltY, rotation,
                                               width, height, timestamp, state,
                                               isEraser, isBarrelButton1, isBarrelButton2, isInverted);
            
            // Notify all listeners
            for (StylusListener listener : listeners) {
                try {
                    listener.onStylusEvent(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Check if stylus/pen is available
     */
    public static native boolean isStylusAvailable();
    
    /**
     * Get maximum supported stylus inputs
     */
    public static native int getMaxStylusPoints();
}
