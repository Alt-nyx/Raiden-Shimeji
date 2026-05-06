import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/* RaidenMini - Mascotte de bureau interactive optimisée */
public class RaidenMini extends JFrame {
    private int x, y, frame = 1, animStep = 0, totalImages = 0;
    private final int IMAGE_SIZE = 360, DECALAGE_SOL_BASE = 325;
    private boolean isDragging = false, hasMoved = false;
    private String state = "IDLE", lastState = "IDLE", targetState = ""; 
    
    private JLabel label;
    private List<JWindow> menuButtons = new ArrayList<>();
    private List<ImageIcon> framesCache = new ArrayList<>(); 
    private int lastFrameRendered = -1;

    public RaidenMini() {
        countImages(); 
        
        setUndecorated(true);
        setType(java.awt.Window.Type.UTILITY); 
        // TEST : Si elle est toujours invisible, change (0,0,0,0) par (255,0,0,100) pour voir un carré rouge
        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(true);
        setSize(IMAGE_SIZE, IMAGE_SIZE);
        setLocationRelativeTo(null);
        setFocusableWindowState(false);
        setTitle("RaidenMini");
        
        ((JPanel)getContentPane()).setOpaque(false);
        getContentPane().setBackground(new Color(0, 0, 0, 0));
        getContentPane().setLayout(new BorderLayout());
        getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        getRootPane().setDoubleBuffered(true);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { exitSequence(); }
        });

        JPanel clickPanel = new JPanel(new BorderLayout());
        clickPanel.setOpaque(false);
        clickPanel.setDoubleBuffered(true); 
        add(clickPanel);

        label = new JLabel();
        label.setOpaque(false);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        clickPanel.add(label, BorderLayout.CENTER);

        setupInteractions(clickPanel);

        // --- Diagnostic de chargement ---
        if (totalImages > 0 && !framesCache.isEmpty()) {
            label.setIcon(framesCache.get(0));
            System.out.println("Image 1 injectée au démarrage.");
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

        try {
            Runtime.getRuntime().exec("hyprctl keyword windowrule noborder,RaidenMini");
        } catch (Exception e) {} 
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
                System.out.println("Images trouvées : " + totalImages);
            }
        } else {
            System.out.println("ERREUR : Dossier img/ introuvable !");
        }
    }

    private void updateUIFrame() {
        if (totalImages == 0 || framesCache.isEmpty()) return; 
        if (frame > totalImages) frame = 1;

        if (frame != lastFrameRendered) {
            label.setIcon(framesCache.get(frame - 1));
            lastFrameRendered = frame;
            label.repaint(); 
        }
    }

    private void exitSequence() {
        state = "EXIT";
        hideHoverMenu();
        try { label.setIcon(new ImageIcon("img/shime19.png")); } catch(Exception e) {}
        new Timer(1000, e -> System.exit(0)).start();
    }

    private void setupInteractions(JPanel cp) {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem quitItem = new JMenuItem("Faire partir Raiden");
        quitItem.addActionListener(e -> exitSequence());
        contextMenu.add(quitItem);

        cp.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { showHoverMenu(); }
            @Override public void mouseExited(MouseEvent e) {
                new Timer(200, evt -> { if (!isMouseOverMenu() && !isMouseOverRaiden()) hideHoverMenu(); }).start();
            }
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (state.equals("FALL") || state.equals("EXIT")) return;
                    isDragging = true; hasMoved = false; hideHoverMenu();
                } else { contextMenu.show(cp, e.getX(), e.getY()); }
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
                    x = p.x - 180; y = p.y - 180;
                    setLocation(x, y);
                }
            }
        });
    }

    private int getSol() {
        int adj = switch(state) {
            case "ASSISE", "SUCETTE", "COEUR", "TRANS_ASSOIR" -> 35;
            case "TRANS_DODO" -> 45;
            case "DODO" -> 65;
            default -> 0;
        };
        return Toolkit.getDefaultToolkit().getScreenSize().height - (DECALAGE_SOL_BASE - adj);
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
                x -= 12; frame = ((animStep / 6) % 2 == 0) ? 2 : 3; 
                if (x <= -180) { state = "SAUT_MUR"; animStep = 0; }
            }
            case "TRANS_ASSOIR" -> {
                frame = 14; 
                if (animStep > 10) { state = targetState.isEmpty() ? "ASSISE" : targetState; targetState = ""; animStep = 0; } 
            }
            case "ASSISE" -> frame = (new int[]{5, 6, 7, 8})[(animStep / 40) % 4];
            case "SUCETTE" -> {
                frame = ((animStep / 30) % 2 == 0) ? 9 : 10;
                if (animStep > 150) { state = "COEUR"; animStep = 0; }
            }
            case "COEUR" -> { frame = 15; if (animStep > 60) { state = "SUCETTE"; animStep = 0; } }
            case "SAUT_MUR" -> { frame = 17; x = 0; if (animStep > 5) { state = "GRIMPE"; animStep = 0; } }
            case "GRIMPE" -> {
                y -= 10; frame = 18; x = 0;
                if (y < 100 || animStep > 25) { state = "FALL"; animStep = 0; } 
            }
            case "TRANS_DODO" -> { frame = 16; if (animStep > 15) { state = "DODO"; animStep = 0; } }
            case "DODO" -> frame = (animStep < 30) ? 11 : (animStep < 60) ? 12 : ((animStep / 50) % 2 == 0) ? 13 : 12;
            default -> frame = 1;
        }
        if (x < -IMAGE_SIZE) x = Toolkit.getDefaultToolkit().getScreenSize().width;
    }

    private void showHoverMenu() {
        if (!menuButtons.isEmpty() || state.equals("EXIT")) return;
        String[] modes = {"DODO", "SUCETTE", "ASSISE", "MARCHE"};
        String[] icons = {"img/sleep-icon.png", "img/sucette-icon.png", "img/assis-icon.png", "img/walk-icon.png"};
        
        for (int i = 0; i < modes.length; i++) {
            final String cible = modes[i];
            JWindow btn = new JWindow();
            btn.setBackground(new Color(0, 0, 0, 0));
            double angle = Math.toRadians(-128 + (i * 25)); 
            btn.setLocation(getX()+180 + (int)(Math.cos(angle)*150)-25, getY()+180 + (int)(Math.sin(angle)*145)-25);

            JLabel l = new JLabel(new ImageIcon(new ImageIcon(icons[i]).getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
            btn.add(l); btn.setSize(50, 50); btn.setAlwaysOnTop(true); btn.setVisible(true); btn.toFront();
            l.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    targetState = cible;
                    state = cible.equals("DODO") ? "TRANS_DODO" : (cible.equals("MARCHE") ? "MARCHE" : "TRANS_ASSOIR");
                    animStep = 0; hideHoverMenu();
                }
            });
            menuButtons.add(btn);
        }
    }

    private boolean isMouseOverMenu() { return menuButtons.stream().anyMatch(b -> b.getBounds().contains(MouseInfo.getPointerInfo().getLocation())); }
    private boolean isMouseOverRaiden() { return getBounds().contains(MouseInfo.getPointerInfo().getLocation()); }
    private void hideHoverMenu() { menuButtons.forEach(Window::dispose); menuButtons.clear(); }

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

    public static void main(String[] args) {
    System.setProperty("sun.java2d.xrender", "true");
    System.setProperty("sun.java2d.opengl", "false");

    SwingUtilities.invokeLater(() -> {
        RaidenMini rm = new RaidenMini();
        // Optionnel : force encore un coup le "sans ombre"
        rm.getRootPane().putClientProperty("Window.shadow", Boolean.FALSE);
    });
}
}