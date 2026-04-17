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
    
    // HUD data for current stylus
    private static volatile int currentPressure = 0;
    private static volatile int currentTiltX = 0;
    private static volatile int currentTiltY = 0;
    private static volatile int currentStylusId = -1;
    private static volatile boolean isEraser = false;
    private static volatile boolean isBarrel1 = false;
    private static volatile boolean isBarrel2 = false;
    
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
        
        // BOTTOM: Info panel with HUD and debug log
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.BLACK);
        
        // LEFT: HUD panel showing pressure/tilt
        JPanel hudPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintHUD((Graphics2D) g);
            }
        };
        hudPanel.setPreferredSize(new Dimension(400, 200));
        hudPanel.setBackground(Color.BLACK);
        
        // RIGHT: Debug log
        debugLog = new JTextArea(8, 70);
        debugLog.setEditable(false);
        debugLog.setFocusable(false);
        debugLog.setFont(new Font("Monospaced", Font.PLAIN, 14));
        debugLog.setBackground(Color.BLACK);
        debugLog.setForeground(Color.GREEN);
        debugLog.setCaretColor(Color.GREEN);
        JScrollPane scrollPane = new JScrollPane(debugLog);
        scrollPane.setFocusable(false);
        scrollPane.setBorder(null);
        
        bottomPanel.add(hudPanel, BorderLayout.WEST);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        
        frame.add(drawPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
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
            
            // Update HUD data
            currentStylusId = event.id;
            currentPressure = event.pressurePercent;
            currentTiltX = event.tiltX;
            currentTiltY = event.tiltY;
            isEraser = event.isEraser;
            isBarrel1 = event.isBarrelButton1;
            isBarrel2 = event.isBarrelButton2;
            
            // Log detailed info with raw pressure
            if (event.state == FastStylus.State.DOWN || event.state == FastStylus.State.MOVE) {
                log(String.format("[STYLUS] id=%d pos=(%d,%d) pressure=%d%% tilt=(%d,%d) eraser=%s barrel1=%s barrel2=%s",
                    event.id, event.x, event.y, event.pressurePercent, 
                    event.tiltX, event.tiltY,
                    event.isEraser, event.isBarrelButton1, event.isBarrelButton2));
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
    
    private static void paintHUD(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int pad = 10;
        int w = 380;
        int h = 180;
        
        // Background
        g.setColor(Color.DARK_GRAY);
        g.fillRoundRect(pad, pad, w, h, 10, 10);
        g.setColor(Color.GRAY);
        g.drawRoundRect(pad, pad, w, h, 10, 10);
        
        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("FastStylus HUD", pad + 10, pad + 25);
        
        // Pressure bar
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Pressure:", pad + 10, pad + 55);
        
        int barX = pad + 100;
        int barY = pad + 40;
        int barW = 200;
        int barH = 20;
        
        // Pressure bar background
        g.setColor(Color.GRAY);
        g.fillRect(barX, barY, barW, barH);
        
        // Pressure bar fill (green to red)
        int fillW = (currentPressure * barW) / 100;
        Color pressureColor = currentPressure > 80 ? Color.RED : 
                             currentPressure > 50 ? Color.ORANGE : Color.GREEN;
        g.setColor(pressureColor);
        g.fillRect(barX, barY, fillW, barH);
        
        // Pressure value
        g.setColor(Color.WHITE);
        g.drawRect(barX, barY, barW, barH);
        g.drawString(currentPressure + "%", barX + barW + 10, barY + 15);
        
        // Tilt visualization
        g.drawString("Tilt X:", pad + 10, pad + 85);
        g.drawString(currentTiltX + "-¦", pad + 100, pad + 85);
        
        g.drawString("Tilt Y:", pad + 160, pad + 85);
        g.drawString(currentTiltY + "-¦", pad + 250, pad + 85);
        
        // Tilt circle visualization
        int cx = pad + 200;
        int cy = pad + 130;
        int r = 35;
        
        // Circle
        g.setColor(Color.GRAY);
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        g.drawLine(cx - r, cy, cx + r, cy);
        g.drawLine(cx, cy - r, cx, cy + r);
        
        // Tilt dot
        if (currentStylusId >= 0) {
            float tx = currentTiltX / 90.0f; // -1 to 1
            float ty = currentTiltY / 90.0f; // -1 to 1
            int dotX = cx + (int)(tx * r * 0.8);
            int dotY = cy - (int)(ty * r * 0.8); // Y inverted for screen coords
            
            g.setColor(Color.CYAN);
            g.fillOval(dotX - 6, dotY - 6, 12, 12);
            g.setColor(Color.WHITE);
            g.drawOval(dotX - 6, dotY - 6, 12, 12);
        }
        
        // Status indicators
        int statusY = pad + 170;
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        
        g.setColor(isEraser ? Color.RED : Color.DARK_GRAY);
        g.fillRoundRect(pad + 10, statusY - 12, 60, 18, 5, 5);
        g.setColor(Color.WHITE);
        g.drawString("ERASER", pad + 18, statusY);
        
        g.setColor(isBarrel1 ? Color.GREEN : Color.DARK_GRAY);
        g.fillRoundRect(pad + 80, statusY - 12, 60, 18, 5, 5);
        g.setColor(Color.WHITE);
        g.drawString("BTN1", pad + 92, statusY);
        
        g.setColor(isBarrel2 ? Color.GREEN : Color.DARK_GRAY);
        g.fillRoundRect(pad + 150, statusY - 12, 60, 18, 5, 5);
        g.setColor(Color.WHITE);
        g.drawString("BTN2", pad + 162, statusY);
    }
}
