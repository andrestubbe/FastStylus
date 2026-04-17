package faststylus;

import fastcore.FastCore;
import java.util.ArrayList;
import java.util.List;

/**
 * FastStylus - Native Stylus/Pen Input for Java
 * 
 * <p>Captures stylus events with native Windows Pointer API via JNI.
 * Supports pressure, tilt, eraser, hover, and barrel buttons for Surface Pro and Wacom devices.</p>
 * 
 * <p><b>Compatible Devices:</b></p>
 * <ul>
 *   <li>Microsoft Surface Pro (all generations with pen)</li>
 *   <li>Wacom Bamboo Ink / Ink Plus</li>
 *   <li>Windows Ink compatible styluses</li>
 * </ul>
 * 
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Pressure sensitivity (0-1024 raw, 0-100% processed)</li>
 *   <li>Tilt X/Y angles (-90° to +90°)</li>
 *   <li>Rotation/orientation (0-360°)</li>
 *   <li>Eraser detection</li>
 *   <li>Barrel buttons (2 buttons)</li>
 *   <li>Hover tracking</li>
 * </ul>
 * 
 * <p><b>Requirements:</b></p>
 * <ul>
 *   <li>Windows 8 or later</li>
 *   <li>Java 17+</li>
 *   <li>faststylus native library (DLL)</li>
 * </ul>
 * 
 * @version 1.0.0
 * @see StylusEvent
 * @see StylusListener
 */
public class FastStylus {
    
    static {
        FastCore.loadLibrary("faststylus");
    }
    
    /**
     * Single stylus event - immutable data class containing all sensor data
     * from a stylus input sample.
     * 
     * <p>Created by {@link FastStylus#poll()} and passed to listeners via
     * {@link StylusListener#onStylusEvent(StylusEvent)}.</p>
     * 
     * <p>All fields are public final for direct access. The class is immutable
     * - once created, values cannot change.</p>
     */
    public static final class StylusEvent {
        /** Pointer ID - unique identifier for each stylus input point */
        public final int id;
        /** X coordinate in window client pixels */
        public final int x;
        /** Y coordinate in window client pixels */
        public final int y;
        /** Raw pressure value from Windows API (0-1024) */
        public final int pressure;
        /** Pressure converted to percentage (0-100%) */
        public final int pressurePercent;
        /** Tilt X angle in degrees (-90 to +90) */
        public final int tiltX;
        /** Tilt Y angle in degrees (-90 to +90) */
        public final int tiltY;
        /** Rotation/orientation in degrees (0-360) */
        public final int rotation;
        /** Contact width in pixels (estimated from pressure, 2-22px) */
        public final int width;
        /** Contact height in pixels (estimated from pressure, 2-22px) */
        public final int height;
        /** Timestamp in milliseconds (system tick count) */
        public final long timestamp;
        /** Input state: HOVER, DOWN, MOVE, or UP */
        public final State state;
        /** True if pen is in eraser mode */
        public final boolean isEraser;
        /** True if barrel button 1 is pressed */
        public final boolean isBarrelButton1;
        /** True if barrel button 2 is pressed */
        public final boolean isBarrelButton2;
        /** True if pen is inverted (eraser end down) */
        public final boolean isInverted;
        
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
    
    /**
     * Stylus input state enumeration
     * 
     * <p>State transitions typically follow:</p>
     * <pre>
     * HOVER → DOWN → MOVE → UP → HOVER
     *              ↺_____↵
     * </pre>
     * 
     * <p><b>States:</b></p>
     * <ul>
     *   <li>{@code HOVER} - Pen near screen but not touching (proximity hover)</li>
     *   <li>{@code DOWN} - Pen just touched screen (initial contact)</li>
     *   <li>{@code MOVE} - Pen dragging on screen (continuous contact)</li>
     *   <li>{@code UP} - Pen lifted from screen (contact ended)</li>
     * </ul>
     */
    public enum State { 
        /** Pen hovering near screen surface */ HOVER, 
        /** Initial contact with screen */ DOWN, 
        /** Dragging/moving while in contact */ MOVE, 
        /** Pen lifted, contact ended */ UP 
    }
    
    // ============================================================================
    // Native Methods - Windows Pointer API (JNI)
    // ============================================================================
    
    /**
     * Native: Initialize stylus input for a window
     * @param hwnd Native window handle
     */
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
     * Interface for receiving stylus input events
     * 
     * <p>Implement this interface and register via {@link #addListener(StylusListener)}
     * to receive stylus events.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * stylus.addListener(event -> {
     *     if (event.state == FastStylus.State.DOWN) {
     *         System.out.println("Pen touched at " + event.x + "," + event.y);
     *     }
     * });
     * </pre>
     */
    public interface StylusListener {
        /**
         * Called when a stylus event occurs
         * @param event The stylus event containing all input data
         */
        void onStylusEvent(StylusEvent event);
    }
    
    /**
     * Create a FastStylus instance for a Swing window
     * 
     * <p>Searches for the native window handle using the frame's title.
     * Retries multiple times to handle window creation delays.</p>
     * 
     * <p><b>Usage:</b></p>
     * <pre>
     * JFrame frame = new JFrame("My App");
     * frame.setVisible(true);
     * FastStylus stylus = FastStylus.create(frame);
     * </pre>
     * 
     * @param frame The JFrame to capture stylus input for
     * @return New FastStylus instance connected to the window
     * @throws RuntimeException if window handle cannot be found
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
     * Add a listener to receive stylus events
     * 
     * @param listener The listener to add
     * @see StylusListener
     */
    public void addListener(StylusListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a previously added listener
     * 
     * @param listener The listener to remove
     */
    public void removeListener(StylusListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Start the stylus polling thread (~120Hz)
     * 
     * <p>Creates a background daemon thread that polls native stylus state
     * and dispatches events to all registered listeners.</p>
     * 
     * <p>No-op if already running.</p>
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
     * 
     * <p>Signals the polling thread to stop. May take up to ~8ms to complete.</p>
     */
    public void stop() {
        running = false;
    }
    
    /**
     * Poll for stylus events once (blocking)
     * 
     * <p>Processes native messages and dispatches events to listeners.
     * Called automatically by the polling thread, but can be called manually
     * for synchronous processing.</p>
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
     * Check if stylus/pen hardware is available
     * 
     * @return true if Pointer API is available (Windows 8+), false otherwise
     */
    public static native boolean isStylusAvailable();
    
    /**
     * Get maximum supported concurrent stylus inputs
     * 
     * @return Maximum number of stylus points (typically 10)
     */
    public static native int getMaxStylusPoints();
}
