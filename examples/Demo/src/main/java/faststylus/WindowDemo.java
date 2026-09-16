package faststylus;

import fasttheme.FastTheme;
import faststylus.FastStylus;
import faststylus.FastStylus.StylusEvent;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * WindowDemo - Minimal white pen on dark canvas
 */
public class WindowDemo extends JFrame {
    
    private DrawCanvas canvas;
    private List<Point> currentStroke = new ArrayList<>();
    private List<List<Point>> allStrokes = new ArrayList<>();
    
    public WindowDemo() {
        setTitle("WindowDemo - White Pen");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // 60% of screen size, centered
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int)(screenSize.width * 0.6);
        int height = (int)(screenSize.height * 0.6);
        setSize(width, height);
        setLocationRelativeTo(null); // Center on screen
        
        System.out.println("[DEBUG] Window size: " + width + "x" + height + " (60% of " + screenSize.width + "x" + screenSize.height + ")");
        
        // Drawing canvas
        canvas = new DrawCanvas();
        setContentPane(canvas);
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (visible && !isVisible()) {
            super.setVisible(true);
            System.out.println("[DEBUG] Window visible");
            
            // Apply FastTheme styling
            long hwnd = FastTheme.getWindowHandle(this);
            System.out.println("[DEBUG] HWND = " + hwnd);
            
            if (hwnd != 0) {
                FastTheme.setWindowTransparency(hwnd, 204);
                FastTheme.setTitleBarColor(hwnd, 12, 12, 12);
                FastTheme.setTitleBarTextColor(hwnd, 255, 255, 255);
                FastTheme.setTitleBarDarkMode(hwnd, true);
                System.out.println("[DEBUG] Theme applied");
            }
            
            // Init FastStylus
            System.out.println("[DEBUG] Creating FastStylus...");
            FastStylus stylus = FastStylus.create(this);
            boolean available = FastStylus.isStylusAvailable();
            System.out.println("[DEBUG] Stylus available = " + available);
            
            // Stylus event listener with debug
            stylus.addListener(event -> {
                System.out.println("[STYLUS] state=" + event.state + " x=" + event.x + " y=" + event.y + " pressure=" + event.pressurePercent + "%");
                
                Point p = new Point(event.x, event.y);
                
                switch (event.state) {
                    case DOWN:
                        currentStroke = new ArrayList<>();
                        currentStroke.add(p);
                        break;
                    case MOVE:
                        if (!currentStroke.isEmpty()) {
                            currentStroke.add(p);
                        }
                        break;
                    case UP:
                        if (!currentStroke.isEmpty()) {
                            allStrokes.add(new ArrayList<>(currentStroke));
                            currentStroke.clear();
                            System.out.println("[DEBUG] Stroke saved, total strokes: " + allStrokes.size());
                        }
                        break;
                    case HOVER:
                        // No drawing on hover
                        break;
                }
                
                SwingUtilities.invokeLater(() -> canvas.repaint());
            });
            
            System.out.println("[DEBUG] Starting stylus...");
            stylus.start();
            System.out.println("[DEBUG] Stylus started");
            
        } else {
            super.setVisible(visible);
        }
    }
    
    class DrawCanvas extends JPanel {
        private Point lastMousePoint = null;
        
        public DrawCanvas() {
            setBackground(new Color(12, 12, 12));
            
            // Mouse dragging for testing
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    lastMousePoint = e.getPoint();
                    currentStroke = new ArrayList<>();
                    currentStroke.add(lastMousePoint);
                    System.out.println("[MOUSE] DOWN at " + lastMousePoint.x + "," + lastMousePoint.y);
                    repaint();
                }
                
                @Override
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    if (!currentStroke.isEmpty()) {
                        allStrokes.add(new ArrayList<>(currentStroke));
                        System.out.println("[MOUSE] UP - stroke saved, total: " + allStrokes.size());
                        currentStroke.clear();
                    }
                    lastMousePoint = null;
                    repaint();
                }
            });
            
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseDragged(java.awt.event.MouseEvent e) {
                    Point p = e.getPoint();
                    currentStroke.add(p);
                    System.out.println("[MOUSE] DRAG to " + p.x + "," + p.y + " (stroke size: " + currentStroke.size() + ")");
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            // Draw completed strokes
            for (List<Point> stroke : allStrokes) {
                drawStroke(g2, stroke);
            }
            
            // Draw current stroke
            if (!currentStroke.isEmpty()) {
                drawStroke(g2, currentStroke);
            }
        }
        
        private void drawStroke(Graphics2D g2, List<Point> stroke) {
            if (stroke.size() < 2) {
                // Draw single point as small circle
                if (stroke.size() == 1) {
                    Point p = stroke.get(0);
                    g2.fillOval(p.x - 2, p.y - 2, 4, 4);
                    System.out.println("[DRAW] Single point at " + p.x + "," + p.y);
                }
                return;
            }
            for (int i = 1; i < stroke.size(); i++) {
                Point p1 = stroke.get(i - 1);
                Point p2 = stroke.get(i);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            System.out.println("[DRAW] Stroke with " + stroke.size() + " points");
        }
    }
    
    public static void main(String[] args) {
        // Disable UI scaling for 1:1 pixel mapping
        System.setProperty("sun.java2d.uiScale", "1.0");
        System.out.println("[DEBUG] UI Scale set to 1.0");
        
        System.out.println("[DEBUG] Starting WindowDemo...");
        SwingUtilities.invokeLater(() -> {
            new WindowDemo().setVisible(true);
        });
    }
}
