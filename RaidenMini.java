import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/* RaidenMini - Mascotte de bureau interactive.*/
public class RaidenMini extends JFrame {
    private int x, y;
    private int frame = 1;
    private int animStep = 0;
    private int totalImages = 0;
    private final int IMAGE_SIZE = 360;
    private final int DECALAGE_SOL_BASE = 325;

    private boolean isDragging = false;
    private boolean hasMoved = false;
    private String state = "IDLE";
    private String lastState = "IDLE";
    private String targetState = ""; 
    
    private JLabel label;
    private List<JWindow> menuButtons = new ArrayList<>();
    private List<ImageIcon> framesCache = new ArrayList<>(); 
    private int lastFrameRendered = -1;

    public RaidenMini() {
        countImages();
        
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(true);
        setSize(IMAGE_SIZE, IMAGE_SIZE);
        setLocationRelativeTo(null);
        setType(java.awt.Window.Type.NORMAL); 

        setIconImage(new ImageIcon("img/shime0.png").getImage());
        setTitle("Raiden Mini"); 

        // --- GESTION DE LA FERMETURE (Alt+F4 / Barre des tâches) ---
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitSequence(); // Lance l'animation triste avant de quitter
            }
        });

        x = getX(); y = getY();

        JPanel clickPanel = new JPanel(new BorderLayout());
        clickPanel.setOpaque(false);
        add(clickPanel);

        label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        clickPanel.add(label, BorderLayout.CENTER);

        // Menu clic droit[cite: 3]
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem quitItem = new JMenuItem("Faire partir Raiden");
        quitItem.addActionListener(e -> exitSequence()); // Appel de la séquence triste ici aussi
        contextMenu.add(quitItem);

        // Interaction souris
        clickPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { showHoverMenu(); }
            @Override
            public void mouseExited(MouseEvent e) {
                Timer checkExit = new Timer(200, evt -> {
                    if (!isMouseOverMenu() && !isMouseOverRaiden()) hideHoverMenu();
                });
                checkExit.setRepeats(false);
                checkExit.start();
            }
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (state.equals("FALL") || state.equals("EXIT")) return;
                    isDragging = true;
                    hasMoved = false;
                    hideHoverMenu();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    contextMenu.show(clickPanel, e.getX(), e.getY());
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && isDragging) {
                    isDragging = false;
                    int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
                    if (hasMoved && y < (screenHeight / 2)) {
                        lastState = state; state = "FALL";    
                    } else if (!hasMoved) { changeState(); }
                }
            }
        });

        clickPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    hasMoved = true;
                    Point p = MouseInfo.getPointerInfo().getLocation();
                    x = p.x - (IMAGE_SIZE / 2);
                    y = p.y - (IMAGE_SIZE / 2);
                    setLocation(x, y);
                }
            }
        });

        new Timer(150, e -> { 
            if (!state.equals("EXIT")) { // On stop la logique si elle est en train de partir
                applyLogic(); 
                updateUIFrame(); 
            }
        }).start();

        setVisible(true);
    }

    /**
     * Séquence de fermeture : affiche Raiden triste (shime19) avant de quitter.
     */
    private void exitSequence() {
        state = "EXIT";
        hideHoverMenu();
        // Affiche l'image 19 (triste) immédiatement
        label.setIcon(new ImageIcon("img/shime19.png"));
        
        // Petit délai de 1 seconde pour voir sa réaction avant de couper
        Timer delayQuit = new Timer(1000, e -> System.exit(0));
        delayQuit.setRepeats(false);
        delayQuit.start();
    }

    private int getSol() {
        int adj = 0;
        switch (state) {
            case "ASSISE": case "SUCETTE": case "COEUR": case "TRANS_ASSOIR": adj = 35; break;
            case "TRANS_DODO": adj = 45; break;
            case "DODO": adj = 65; break; 
            default: adj = 0; break;
        }
        return Toolkit.getDefaultToolkit().getScreenSize().height - (DECALAGE_SOL_BASE - adj);
    }

    private void applyLogic() {
        if (isDragging) return;
        int sol = getSol();

        if (state.equals("FALL")) {
            if (y < sol) {
                y += 25; frame = 4;
                if (y > sol) y = sol;
            } else { y = sol; state = lastState; }
            setLocation(x, y);
            return;
        }

        y = sol;
        setLocation(x, y);
        animStep++;

        switch (state) {
            case "MARCHE": 
                x -= 12; frame = ((animStep / 6) % 2 == 0) ? 2 : 3; 
                if (x <= -180) { state = "SAUT_MUR"; animStep = 0; }
                break;
            case "TRANS_ASSOIR": 
                frame = 14; 
                if (animStep > 10) { 
                    state = (targetState.isEmpty()) ? "ASSISE" : targetState;
                    targetState = ""; animStep = 0; 
                } 
                break;
            case "ASSISE": frame = (new int[]{5, 6, 7, 8})[(animStep / 40) % 4]; break;
            case "SUCETTE": 
                frame = ((animStep / 30) % 2 == 0) ? 9 : 10;
                if (animStep > 150) { state = "COEUR"; animStep = 0; }
                break;
            case "COEUR": frame = 15; if (animStep > 60) { state = "SUCETTE"; animStep = 0; } break;
            case "SAUT_MUR": frame = 17; x = 0; if (animStep > 5) { state = "GRIMPE"; animStep = 0; } break;
            case "GRIMPE":
                y -= 10; frame = 18; x = 0;
                if (y < 100 || animStep > 25) { state = "FALL"; animStep = 0; } 
                break;
            case "TRANS_DODO": frame = 16; if (animStep > 15) { state = "DODO"; animStep = 0; } break;
            case "DODO": frame = (animStep < 30) ? 11 : (animStep < 60) ? 12 : ((animStep / 50) % 2 == 0) ? 13 : 12; break;
            default: frame = 1; break;
        }
        if (x < -IMAGE_SIZE) { x = Toolkit.getDefaultToolkit().getScreenSize().width; }
    }

    private void showHoverMenu() {
        if (!menuButtons.isEmpty() || state.equals("EXIT")) return;
        
        String[] modes = {"DODO", "SUCETTE", "ASSISE", "MARCHE"};
        String[] icons = {"img/sleep-icon.png", "img/sucette-icon.png", "img/assis-icon.png", "img/walk-icon.png"};
        
        int centerX = getX() + (IMAGE_SIZE / 2);
        int centerY = getY() + (IMAGE_SIZE / 2);

        for (int i = 0; i < modes.length; i++) {
            final String cible = modes[i];
            int bubbleSize = 50; 
            JWindow btn = new JWindow();
            btn.setBackground(new Color(0, 0, 0, 0));
            
            double angle = Math.toRadians(-128 + (i * 25)); 
            int btnX = centerX + (int)(Math.cos(angle) * 150) - (bubbleSize / 2);
            int btnY = centerY + (int)(Math.sin(angle) * 145) - (bubbleSize / 2);

            JPanel bubble = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(2, 2, bubbleSize - 5, bubbleSize - 5);
                    g2.setColor(new Color(210, 210, 210));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(2, 2, bubbleSize - 5, bubbleSize - 5);
                }
            };
            bubble.setLayout(new BorderLayout());
            bubble.setOpaque(false);

            Image scaled = new ImageIcon(icons[i]).getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaled));
            bubble.add(iconLabel, BorderLayout.CENTER);
            
            btn.add(bubble);
            btn.setSize(bubbleSize, bubbleSize);
            btn.setLocation(btnX, btnY);
            btn.setAlwaysOnTop(true);
            btn.setVisible(true);

            iconLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    targetState = cible;
                    if (cible.equals("DODO")) state = "TRANS_DODO";
                    else if (cible.equals("ASSISE") || cible.equals("SUCETTE")) state = "TRANS_ASSOIR";
                    else state = cible;
                    animStep = 0; hideHoverMenu();
                }
            });
            menuButtons.add(btn);
        }
    }

    private boolean isMouseOverMenu() {
        Point p = MouseInfo.getPointerInfo().getLocation();
        return menuButtons.stream().anyMatch(btn -> btn.getBounds().contains(p));
    }

    private boolean isMouseOverRaiden() {
        PointerInfo pi = MouseInfo.getPointerInfo();
        return pi != null && getBounds().contains(pi.getLocation());
    }

    private void hideHoverMenu() {
        menuButtons.forEach(Window::dispose);
        menuButtons.clear();
    }

    private void countImages() {
        File dir = new File("img/");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.startsWith("shime") && name.endsWith(".png"));
            if (files != null) {
                totalImages = files.length;
                framesCache.clear();
                for (int i = 1; i <= totalImages; i++) {
                    framesCache.add(new ImageIcon("img/shime" + i + ".png"));
                }
            }
        }
    }

    private void changeState() {
        animStep = 0;
        state = switch (state) {
            case "IDLE" -> "MARCHE";
            case "MARCHE" -> "TRANS_ASSOIR";
            case "ASSISE" -> "SUCETTE";
            case "SUCETTE" -> "TRANS_DODO";
            default -> "IDLE";
        };
    }

    private void updateUIFrame() {
        if (frame > totalImages && totalImages > 0) frame = 1;
        if (frame != lastFrameRendered && !framesCache.isEmpty()) {
            label.setIcon(framesCache.get(frame - 1));
            lastFrameRendered = frame;
        }
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.xrender", "false");
        
        SwingUtilities.invokeLater(RaidenMini::new);
    }
}