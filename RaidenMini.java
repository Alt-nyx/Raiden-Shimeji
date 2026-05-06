import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RaidenMini extends JFrame {
    // Taille réduite à 150 pour que la "bordure" invisible soit plus petite
    private int x, y, frame = 1, animStep = 0, totalImages = 0;
    private final int IMAGE_SIZE = 150, DECALAGE_SOL_BASE = 140; 
    private boolean isDragging = false, hasMoved = false;
    private String state = "IDLE", lastState = "IDLE", targetState = ""; 
    
    private JLabel label;
    private List<JWindow> menuButtons = new ArrayList<>();
    private List<ImageIcon> framesCache = new ArrayList<>(); 
    private int lastFrameRendered = -1;

    public RaidenMini() {
        // 1. On donne le titre AVANT tout pour qu'Hyprland le reconnaisse
        setTitle("RaidenMini");
        countImages(); 
        
        setUndecorated(true);
        setType(java.awt.Window.Type.UTILITY); 
        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(true);
        setSize(IMAGE_SIZE, IMAGE_SIZE);
        setLocationRelativeTo(null);
        
        // Empêche la fenêtre de voler le focus (aide pour le menu et les bordures)
        setFocusableWindowState(false);
        setFocusable(false);

        ((JPanel)getContentPane()).setOpaque(false);
        getRootPane().setWindowDecorationStyle(JRootPane.NONE);

        JPanel clickPanel = new JPanel(new BorderLayout());
        clickPanel.setOpaque(false);
        add(clickPanel);

        label = new JLabel();
        label.setOpaque(false);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        clickPanel.add(label, BorderLayout.CENTER);

        setupInteractions(clickPanel);

        if (totalImages > 0 && !framesCache.isEmpty()) {
            label.setIcon(framesCache.get(0));
        }

        updateUIFrame(); 

        new Timer(150, e -> { 
            if (!state.equals("EXIT")) {
                applyLogic(); 
                updateUIFrame(); 
            }
        }).start();

        setVisible(true);
        x = getX(); y = getY();

        // 2. Injection des règles Hyprland en direct
        try {
            String[] cmds = {
                "hyprctl keyword windowrule \"noborder, ^(RaidenMini)$\"",
                "hyprctl keyword windowrule \"noshadow, ^(RaidenMini)$\"",
                "hyprctl keyword windowrule \"blur off, ^(RaidenMini)$\"",
                "hyprctl keyword windowrule \"float, ^(RaidenMini)$\""
            };
            for(String c : cmds) Runtime.getRuntime().exec(c);
        } catch (Exception e) {} 
    } 

    private void countImages() {
        File dir = new File("img/");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.startsWith("shime") && name.endsWith(".png"));
            if (files != null) {
                totalImages = files.length;
                for (int i = 1; i <= totalImages; i++) {
                    // On redimensionne les images pour qu'elles rentrent dans le nouveau IMAGE_SIZE
                    ImageIcon icon = new ImageIcon("img/shime" + i + ".png");
                    Image img = icon.getImage().getScaledInstance(IMAGE_SIZE, IMAGE_SIZE, Image.SCALE_SMOOTH);
                    framesCache.add(new ImageIcon(img));
                }
            }
        }
    }

    private void updateUIFrame() {
        if (totalImages == 0 || framesCache.isEmpty()) return; 
        if (frame > totalImages) frame = 1;

        if (frame != lastFrameRendered) {
            label.setIcon(framesCache.get(frame - 1));
            lastFrameRendered = frame;
            // paintImmediately évite le clignotement blanc/gris entre les frames
            label.paintImmediately(0, 0, IMAGE_SIZE, IMAGE_SIZE);
        }
    }

    private void setupInteractions(JPanel cp) {
        cp.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { showHoverMenu(); }
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isDragging = true; hasMoved = false; hideHoverMenu();
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (isDragging) {
                    isDragging = false;
                    if (hasMoved && y < (Toolkit.getDefaultToolkit().getScreenSize().height / 2)) {
                        lastState = state; state = "FALL";    
                    } else if (!hasMoved) changeState();
                }
            }
        });

        cp.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    hasMoved = true;
                    Point p = MouseInfo.getPointerInfo().getLocation();
                    x = p.x - (IMAGE_SIZE/2); y = p.y - (IMAGE_SIZE/2);
                    setLocation(x, y);
                }
            }
        });
    }

    private int getSol() {
        return Toolkit.getDefaultToolkit().getScreenSize().height - DECALAGE_SOL_BASE;
    }

    private void applyLogic() {
        if (isDragging) return;
        int sol = getSol();
        if (state.equals("FALL")) {
            if (y < sol) { y += 25; frame = 4; } 
            else { y = sol; state = lastState; }
            setLocation(x, y); return;
        }
        y = sol; setLocation(x, y); animStep++;

        switch (state) {
            case "MARCHE" -> {
                x -= 8; frame = ((animStep / 6) % 2 == 0) ? 2 : 3; 
                if (x <= -IMAGE_SIZE) x = Toolkit.getDefaultToolkit().getScreenSize().width;
            }
            case "ASSISE" -> frame = (new int[]{5, 6, 7, 8})[(animStep / 20) % 4];
            default -> frame = 1;
        }
    }

    private void showHoverMenu() {
        if (!menuButtons.isEmpty()) return;
        String[] modes = {"DODO", "MARCHE", "ASSISE"};
        for (int i = 0; i < modes.length; i++) {
            final String cible = modes[i];
            JWindow btn = new JWindow();
            btn.setBackground(new Color(0, 0, 0, 0));
            btn.setSize(40, 40);
            btn.setLocation(getX() + (i * 45), getY() - 50);

            JButton b = new JButton(cible.substring(0, 1));
            btn.add(b);
            btn.setAlwaysOnTop(true);
            btn.setVisible(true);
            btn.toFront(); // Force le menu devant Raiden
            
            b.addActionListener(e -> { state = cible; hideHoverMenu(); });
            menuButtons.add(btn);
        }
    }

    private void hideHoverMenu() { menuButtons.forEach(Window::dispose); menuButtons.clear(); }

    private void changeState() {
        state = state.equals("IDLE") ? "MARCHE" : (state.equals("MARCHE") ? "ASSISE" : "IDLE");
    }

    private void exitSequence() { System.exit(0); }

    public static void main(String[] args) {
        // Paramètres pour stabiliser Wayland
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.xrender", "false"); 

        // Désactivation du manager de repaint pour stopper les flashs
        RepaintManager.setCurrentManager(new RepaintManager() {
            @Override public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {}
        });

        SwingUtilities.invokeLater(RaidenMini::new);
    }
}