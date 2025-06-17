package pacman.AI;

import javax.swing.*;
import java.awt.*;

public class AIDebugWindow extends JFrame {
    private final JTextArea textArea;

    private static AIDebugWindow instance;

    public AIDebugWindow() {
        setTitle("Pacman AI Debug");
        setSize(400, 300);
        setLocation(50, 50);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);

        add(scrollPane);
        setVisible(true);
    }

    public static AIDebugWindow getInstance() {
        if (instance == null) {
            instance = new AIDebugWindow();
        }
        return instance;
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(message + "\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> textArea.setText(""));
    }
}