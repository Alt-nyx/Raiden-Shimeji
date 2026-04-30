import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RaidenMini extends JWindow {
    private int x, y;
    private int frame = 1;
    private boolean isDragging = false;
    private int animStep = 0;
    private String state = "IDLE";
    private JLabel label;

    private final int IMAGE_SIZE = 360;
    private final int DECALAGE_SOL_BASE = 325; // Base ajustée pour éviter la barre des tâches

    public RaidenMini() {
        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(true);
        setSize(IMAGE_SIZE, IMAGE_SIZE);
        setLocationRelativeTo(null);

        JPanel clickPanel = new JPanel(new BorderLayout());
        clickPanel.setOpaque(false);
        add(clickPanel);

        label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        clickPanel.add(label, BorderLayout.CENTER);

        clickPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isDragging = true;
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (state.equals("GRAB")) {
                        state = "FALL"; // On tombe quand on relâche
                    } else {
                        changeState(); // On change d'état seulement si on n'était pas en train de porter
                    }
                    isDragging = false;
                }
            }
        });

        clickPanel.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                x = p.x - (IMAGE_SIZE / 2);
                y = p.y - (IMAGE_SIZE / 2);
                state = "GRAB"; 
                setLocation(x, y);
                updateUIFrame();
            }
        });

        new Timer(150, e -> {
            applyLogic();
            updateUIFrame();
        }).start();

        setVisible(true);
        x = getX(); y = getY();
    }

    private void changeState() {
        animStep = 0;
        switch (state) {
            case "IDLE":   state = "MARCHE"; break;
            case "MARCHE": state = "ASSISE"; break;
            case "ASSISE": state = "SUCETTE"; break;
            case "SUCETTE": state = "DODO";   break;
            default:       state = "IDLE";   break;
        }
    }

    private int getSol() {
        int ajustementAction = 0;
        switch (state) {
            case "ASSISE":  ajustementAction = 35; break;
            case "SUCETTE": ajustementAction = 35; break;
            case "DODO":    ajustementAction = 65; break; 
            default:        ajustementAction = 0;  break;
        }
        return Toolkit.getDefaultToolkit().getScreenSize().height - (DECALAGE_SOL_BASE - ajustementAction);
    }

    private void applyLogic() {
        int sol = getSol();

        // Gestion de la chute
        if (!isDragging && (state.equals("GRAB") || state.equals("FALL"))) {
            if (y < sol) {
                state = "FALL";
                y += 100; // Vitesse de chute
                if (y > sol) y = sol;
            } else {
                y = sol;
                state = "IDLE";
            }
            setLocation(x, y);
            return;
        }

        if (!isDragging) {
            y = sol;
            setLocation(x, y);
            animStep++;
        }

        // --- ANIMATIONS ---[cite: 1]
        switch (state) {
            case "MARCHE":
                x -= 12;
                frame = ((animStep / 6) % 2 == 0) ? 2 : 3;
                break;
            case "ASSISE":
                int[] seqAssise = {5, 6, 7, 8};
                frame = seqAssise[(animStep / 40) % 4];
                break;
            case "SUCETTE":
                frame = ((animStep / 30) % 2 == 0) ? 9 : 10;
                break;
            case "DODO":
                if (animStep < 30) frame = 11;
                else if (animStep < 60) frame = 12;
                else frame = ((animStep / 50) % 2 == 0) ? 13 : 12;
                break;
            case "GRAB":
            case "FALL":
                frame = 4;
                break;
            default: 
                frame = 1;
                break;
        }

        // Réapparition de l'autre côté de l'écran
        if (!isDragging && x < -IMAGE_SIZE) {
            x = Toolkit.getDefaultToolkit().getScreenSize().width;
        }
    }

    private void updateUIFrame() {
        // Pense à bien vérifier que tes images sont dans le dossier img/
        label.setIcon(new ImageIcon("img/shime" + frame + ".png"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RaidenMini::new);
    }
}
