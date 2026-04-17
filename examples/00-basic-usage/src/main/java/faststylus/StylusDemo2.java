package faststylus;

import fasttheme.FastTheme;
import faststylus.FastStylus;
import faststylus.FastStylus.StylusEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * StylusDemo2 - Minimal white pen on dark canvas
 * 
 * Empty dark window with white stroke when drawing with stylus.
 */
public class StylusDemo2 extends JFrame {
    
    private DrawCanvas canvas;
    private JLabel statusLabel;
    
    // Stroke points
    private List<Point> currentStroke = new ArrayList<>();
    private List<List<Point>> allStrokes = new ArrayList<>();
    
    public StylusDemo2() {
        setTitle("StylusDemo2 - White Pen");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Status label at bottom
        statusLabel = new JLabel("Stylus: waiting...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        // Drawing canvas
        canvas = new DrawCanvas();
        canvas.setBackground(new Color(12, 12, 12)); // Dark background
        
        // Layout
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(12, 12, 12));
        panel.add(canvas, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        
        setContentPane(panel);
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (visible && !isVisible()) {
            super.setVisible(true);
            
            // Apply FastTheme styling
            long hwnd = FastTheme.getWindowHandle(this);
            if (hwnd != 0) {
                FastTheme.setWindowTransparency(hwnd, 240);
                FastTheme.setTitleBarColor(hwnd, 12, 12, 12);
                FastTheme.setTitleBarTextColor(hwnd, 255, 255, 255);
                FastTheme.setTitleBarDarkMode(hwnd, true);
            }
            
            // Init FastStylus
            FastStylus stylus = FastStylus.create(this);
            boolean available = FastStylus.isStylusAvailable();
            statusLabel.setText("Stylus: " + (available ? "ready - draw with pen" : "not detected"));
            
            // Stylus event listener
            stylus.addListener(event -> {
                SwingUtilities.invokeLater(() -> {
                    handleStylusEvent(event);
                });
            });
            
            stylus.start();
            
        } else {
            super.setVisible(visible);
        }
    }
    
    private void handleStylusEvent(StylusEvent event) {
        Point p = new Point(event.x, event.y);
        
        switch (event.state) {
            case DOWN:
                currentStroke = new ArrayList<>();
                currentStroke.add(p);
                statusLabel.setText(String.format("Drawing... pressure=%d%%", event.pressurePercent));
                break;
                
            case MOVE:
                if (!currentStroke.isEmpty()) {
                    currentStroke.add(p);
                    canvas.repaint();
                }
                break;
                
            case UP:
                if (!currentStroke.isEmpty()) {
                    allStrokes.add(new ArrayList<>(currentStroke));
                    currentStroke.clear();
                }
                statusLabel.setText("Stylus: ready");
                break;
                
            case HOVER:
                // Just update status
                statusLabel.setText(String.format("Hover: x=%d y=%d", event.x, event.y));
                break;
        }
        
        canvas.repaint();
    }
    
    /**
     * Canvas for drawing white strokes
     */
    class DrawCanvas extends JPanel {
        
        private BufferedImage buffer;
        
        public DrawCanvas() {
            setOpaque(true);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            
            // Enable antialiasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // White stroke, 3px width
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
            if (stroke.size() < 2) return;
            
            for (int i = 1; i < stroke.size(); i++) {
                Point p1 = stroke.get(i - 1);
                Point p2 = stroke.get(i);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new StylusDemo2().setVisible(true);
        });
    }
}
