package view;

import javax.swing.*;
import java.awt.*;

// hlavni okno: uprostred platno kde se kresli scena, vpravo postranni panel na tlacitka
public class Window extends JFrame {

    private final Panel panel;
    private final JPanel sidePanel;

    public Window(int width, int heigth) {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("PGRF1 2024/2025");
        setLayout(new BorderLayout());

        panel = new Panel(width, heigth);
        JPanel centerWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        centerWrap.add(panel);

        sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        int sideW = 260;
        JScrollPane sideScroll = new JScrollPane(sidePanel);
        sideScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.setPreferredSize(new Dimension(sideW, heigth));
        sideScroll.setBorder(BorderFactory.createEtchedBorder());

        add(centerWrap, BorderLayout.CENTER);
        add(sideScroll, BorderLayout.EAST);
        pack();
        setVisible(true);

        // focus na panel at jdou klavesy (wsad atd)
        panel.setFocusable(true);
        panel.grabFocus();
    }

    public Panel getPanel() {
        return panel;
    }

    // vyceteni tlacitek a checkboxu dela Controller3D
    public JPanel getSidePanel() {
        return sidePanel;
    }
}
