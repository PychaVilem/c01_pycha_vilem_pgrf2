package controller;

import model.Scene;
import raster.ZBuffer;
import rasterize.LineRasterizer;
import rasterize.LineRasterizerGraphics;
import rasterize.TriangelRasterizer;
import renderer.RendererSolid;
import transforms.Camera;
import transforms.Mat4;
import transforms.Mat4OrthoRH;
import transforms.Mat4PerspRH;
import view.Panel;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Controller3D {
    private final Panel panel;
    private final ZBuffer zBuffer;
    private final TriangelRasterizer triangelRasterizer;
    private final RendererSolid renderer;
    private final Scene scene;
    private final LineRasterizer lineRasterizer;

    private Camera camera;
    private boolean perspective = true;

    //todo vykreslit plochu s barevnym prechodem


    public Controller3D(Panel panel) {
        this.panel = panel;
        this.zBuffer = new ZBuffer(panel.getRaster());
        this.lineRasterizer = new LineRasterizerGraphics(panel.getRaster());
        this.triangelRasterizer = new TriangelRasterizer(zBuffer);
        this.renderer = new RendererSolid(lineRasterizer, triangelRasterizer);
        this.scene = new Scene();

        this.camera = new Camera();
        renderer.setViewportSize(panel.getRaster().getWidth(), panel.getRaster().getHeight());

        initListeners();

        drawScene();
    }

    private void initListeners() {
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W -> camera = camera.forward(0.5);
                    case KeyEvent.VK_S -> camera = camera.backward(0.5);
                    case KeyEvent.VK_A -> camera = camera.left(0.5);
                    case KeyEvent.VK_D -> camera = camera.right(0.5);
                    case KeyEvent.VK_P -> perspective = !perspective;
                    default -> {
                        return;
                    }
                }

                drawScene();
            }
        });

        panel.requestFocusInWindow();
    }

    private void drawScene() {
        panel.getRaster().clear();
        zBuffer.clear();

        // view + projekce
        Mat4 view = camera.getViewMatrix();
        int w = panel.getRaster().getWidth();
        int h = panel.getRaster().getHeight();
        double aspect = (double) h / (double) w;

        Mat4 projection;
        if (perspective) {
            projection = new Mat4PerspRH(Math.toRadians(60), aspect, 0.1, 50.0);
        } else {
            projection = new Mat4OrthoRH(20.0, 20.0 * aspect, 0.1, 50.0);
        }

        renderer.setViewMatrix(view);
        renderer.setProjectionMatrix(projection);

        // vykreslení celé scény přes renderer
        for (solid.Solid solid : scene.getSolids()) {
            renderer.render(solid);
        }

        panel.repaint();
    }
}
