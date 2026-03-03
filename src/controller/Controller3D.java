package controller;

import model.Vertex;
import raster.ZBuffer;
import rasterize.TriangelRasterizer;
import transforms.*;
import view.Panel;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Controller3D {
    private final Panel panel;
    private final ZBuffer zBuffer;
    private final TriangelRasterizer triangelRasterizer;

    private int offsetX = 0;
    private int offsetY = 0;
    private int lastClickX = 0;
    private int lastClickY = 0;



    public Controller3D(Panel panel) {
        this.panel = panel;
        this.zBuffer = new ZBuffer(panel.getRaster());
        this.triangelRasterizer = new TriangelRasterizer(zBuffer);

        initListeners();

        drawScene();
    }

    private void initListeners() {
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                final int step = 10;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> offsetY -= step;
                    case KeyEvent.VK_S -> offsetY += step;
                    case KeyEvent.VK_A -> offsetX -= step;
                    case KeyEvent.VK_D -> offsetX += step;
                    case KeyEvent.VK_P -> {
                        offsetX = 0;
                        offsetY = 0;
                    }
                    default -> {
                        return;
                    }
                }

                drawScene();
            }
        });

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                lastClickX = e.getX();
                lastClickY = e.getY();

                // posuň scénu tak, aby bod A trojúhelníku byl v místě kliku
                offsetX = lastClickX - 400;
                offsetY = lastClickY - 0;

                drawScene();
            }
        });

        panel.requestFocusInWindow();
    }

    private void drawScene() {
        panel.getRaster().clear();

       zBuffer.setPixelWithZTest(50,50,0.1, new Col(0xff0000));
        zBuffer.setPixelWithZTest(50,50,0.4, new Col(0x00ff00));

        triangelRasterizer.rasterize(
                new Vertex(400,0,0.5),
                new Vertex(0,300,0.5),
                new Vertex(799,399,0.5)
        );
        triangelRasterizer.rasterize(
                new Vertex(200,0,0.7,new Col(0x00ff00)),
                new Vertex(0,150,0.7,new Col(0x00ff00)),
                new Vertex(799,399,0.7, new Col(0x00ff00))
        );


        panel.repaint();
    }
}
