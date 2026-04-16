package faststylus;

import faststylus.FastStylus;
import faststylus.FastStylus.StylusEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * FastStylus Demo - Pressure-sensitive drawing with tilt visualization
 */
public class StylusDemo {
    
    private static final Map<Integer, Color> stylusColors = new HashMap<>();
    private static BufferedImage canvas;
    private static Graphics2D canvasG;
    private static JPanel drawPanel;
    private static JTextArea debugLog;
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    
    // Track last position for drawing lines
    private static final Map<Integer, Point> lastPositions = new HashMap<>();
    
    public static void main(String[] args) throws Exception {
        log("StylusDemo starting...");
        
        // Create window - full width for drawing
        log("Creating JFrame...");
        JFrame frame = new JFrame("FastStylus Demo - Pressure-Sensitive Drawing");
        frame.setSize(2000, 1400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        // TOP: Full drawing canvas
        drawPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (canvas != null) {
                    g.drawImage(canvas, 0, 0, null);
                }
            }
        };
        drawPanel.setPreferredSize(new Dimension(2000, 1200));
        drawPanel.setBackground(Color.WHITE);
        
        // BOTTOM: Debug log
        debugLog = new JTextArea(8, 80);
        debugLog.setEditable(false);
        debugLog.setFocusable(false);
        debugLog.setFont(new Font("Monospaced", Font.PLAIN, 18));
        debugLog.setBackground(Color.BLACK);
        debugLog.setForeground(Color.GREEN);
        debugLog.setCaretColor(Color.GREEN);
        JScrollPane scrollPane = new JScrollPane(debugLog);
        scrollPane.setPreferredSize(new Dimension(2000, 200));
        scrollPane.setFocusable(false);
        scrollPane.setBorder(null);
        
        frame.add(drawPanel, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.SOUTH);
        frame.setVisible(true);
        log("JFrame visible, title: '" + frame.getTitle() + "'");
        
        // Drawing canvas - white background
        log("Creating canvas...");
        canvas = new BufferedImage(2000, 1200, BufferedImage.TYPE_INT_ARGB);
        canvasG = canvas.createGraphics();
        canvasG.setColor(Color.WHITE);
        canvasG.fillRect(0, 0, 2000, 1200);
        canvasG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Show white background
        drawPanel.repaint();
        
        // Initialize stylus
        log("Initializing FastStylus...");
        FastStylus stylus = FastStylus.create(frame);
        log("FastStylus created successfully");
        
        // Check availability
        log("Checking stylus availability...");
        boolean available = FastStylus.isStylusAvailable();
        log("Stylus available: " + available);
        log("Max stylus inputs: " + FastStylus.getMaxStylusPoints());
        
        if (!available) {
            log("WARNING: No stylus detected! Check Windows Ink settings.");
        } else {
            log("Stylus detected - ready for input");
        }
        
        // Stylus listener for drawing
        log("Adding stylus listener...");
        stylus.addListener(event -> {
            // Assign color per stylus ID
            stylusColors.putIfAbsent(event.id, getColorForId(event.id));
            Color color = stylusColors.get(event.id);
            
            // Log detailed info
            if (event.state == FastStylus.State.DOWN || event.state == FastStylus.State.HOVER) {
                log(String.format("[STYLUS] id=%d pos=(%d,%d) pressure=%d%% tilt=(%d,%d) rot=%d eraser=%s barrel1=%s barrel2=%s %s",
                    event.id, event.x, event.y, event.pressurePercent, 
                    event.tiltX, event.tiltY, event.rotation,
                    event.isEraser, event.isBarrelButton1, event.isBarrelButton2, event.state));
            }
            
            // Draw based on state
            if (event.state == FastStylus.State.DOWN || event.state == FastStylus.State.MOVE) {
                // Draw line from last position
                Point lastPos = lastPositions.get(event.id);
                if (lastPos != null) {
                    // Eraser mode: erase (draw white)
                    if (event.isEraser) {
                        canvasG.setColor(Color.WHITE);
                        int eraserSize = 50 + (event.pressurePercent / 2);  // Pressure affects eraser size
                        canvasG.setStroke(new BasicStroke(eraserSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    } else {
                        // Normal drawing: pressure affects line width
                        canvasG.setColor(color);
                        // Line width: 2-20 based on pressure
                        float width = 2 + (event.pressurePercent / 5.5f);
                        canvasG.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    }
                    
                    canvasG.drawLine(lastPos.x, lastPos.y, event.x, event.y);
                }
                
                // Update last position
                lastPositions.put(event.id, new Point(event.x, event.y));
                
            } else if (event.state == FastStylus.State.UP) {
                // Clear last position on UP
                lastPositions.remove(event.id);
            } else if (event.state == FastStylus.State.HOVER) {
                // Just track hover position (don't draw)
                lastPositions.put(event.id, new Point(event.x, event.y));
            }
            
            // Repaint
            drawPanel.repaint();
        });
        
        // Start stylus polling
        stylus.start();
        log("Stylus polling started. Draw on the screen with your pen!");
        log("Tips:");
        log("- Pressure affects line thickness");
        log("- Eraser tip erases");
        log("- ESC to exit");
        
        // Keyboard listener for exit
        frame.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    stylus.stop();
                    System.exit(0);
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                    // Clear canvas on space
                    canvasG.setColor(Color.WHITE);
                    canvasG.fillRect(0, 0, 2000, 1200);
                    drawPanel.repaint();
                    log("Canvas cleared");
                }
            }
        });
        
        // Animation loop
        while (frame.isVisible()) {
            drawPanel.repaint();
            Thread.sleep(16); // ~60 FPS
        }
    }
    
    private static void log(String msg) {
        String time = timeFormat.format(new Date());
        String line = "[" + time + "] " + msg;
        if (debugLog != null) {
            SwingUtilities.invokeLater(() -> {
                debugLog.append(line + "\n");
                debugLog.setCaretPosition(debugLog.getDocument().getLength());
            });
        }
        System.out.println(line);
    }
    
    private static Color getColorForId(int id) {
        Color[] colors = {
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE,
            Color.MAGENTA, Color.CYAN, Color.PINK, Color.YELLOW
        };
        return colors[id % colors.length];
    }
}
