package lv.rnu.tempmonitor;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Starting point of the program.
 * It only creates the window. All the work is in MainWindow and SerialConnection.
 */
public class Main {

    public static void main(String[] args) {
        // Use the normal look of the operating system (Windows buttons on Windows).
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Not important. Swing then uses its default look.
        }

        // Swing windows must be created on the Swing thread (the "Event Dispatch Thread").
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
