package controller;

import model.Scene;
import raster.ZBuffer;
import rasterize.LineRasterizer;
import rasterize.LineRasterizerGraphics;
import rasterize.TriangelRasterizer;
import renderer.RendererSolid;
import shader.ShaderConstant;
import shader.ShaderInterpolated;
import shader.ShaderTexture;
import transforms.*;
import view.Panel;

import javax.imageio.ImageIO;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class Controller3D {
    private final Panel panel;
    private final ZBuffer zBuffer;
    private final TriangelRasterizer triangelRasterizer;
    private final RendererSolid renderer;
    private final Scene scene;
    private final LineRasterizer lineRasterizer;

    private Camera camera;
    private boolean perspective = true; // true = perspektivní, false = ortogonální (přepínáno klávesou P)
    private boolean wireframeOnly = false; // false = vyplněné plochy, true = drátový model (klávesa M)

    /** Poslední pozice myši při tažení (levé tlačítko) pro rozhlížení. */
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private static final double MOUSE_SENSITIVITY = 0.005; // radiánů na pixel
    private static final double MOVE_STEP = 0.2;
    private static final double ROT_STEP = 0.15;
    private static final double SCALE_UP = 1.15;
    private static final double SCALE_DOWN = 1.0 / 1.15;
    private static final long REDRAW_THROTTLE_MS = 10;
    private long lastDrawTime = 0;
    private BufferedImage texture;
    private ShaderTexture textureShader;

    public Controller3D(Panel panel) {
        this.panel = panel;
        this.zBuffer = new ZBuffer(panel.getRaster());
        this.lineRasterizer = new LineRasterizerGraphics(panel.getRaster());
        this.triangelRasterizer = new TriangelRasterizer(zBuffer);
        this.renderer = new RendererSolid(lineRasterizer, triangelRasterizer);
        this.scene = new Scene();

        // načtení textury (pokud existuje) – zkusíme více cest a formátů
        try {
            BufferedImage tex = null;
            File fileJpg = new File("resources/textures/texture2.jpg");
            File filePng = new File("resources/textures/texture.png");
            if (fileJpg.exists()) {
                tex = ImageIO.read(fileJpg);
            } else if (filePng.exists()) {
                tex = ImageIO.read(filePng);
            }
            if (tex != null) {
                texture = tex;
                textureShader = new ShaderTexture(texture);
                // po úspěšném načtení pošleme texturu i rendereru
                renderer.setTexture(texture);
            } else {
                texture = null;
                textureShader = null;
            }
        } catch (Exception e) {
            texture = null;
            textureShader = null;
        }

        // Pozice (4, 2, 3); azimuth a zenith tak, aby pohled směřoval na střed os (0, 0, 0.5)
        double dx = 0 - 4, dy = 0 - 2, dz = 0.5 - 3;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double azimuth = Math.atan2(dy, dx);
        double zenith = len > 1e-6 ? Math.asin(dz / len) : 0;

        this.camera = new Camera(
                new Vec3D(4, 2, 3),
                azimuth, zenith, 1.0, true
        );
        renderer.setViewportSize(panel.getRaster().getWidth(), panel.getRaster().getHeight());

        initListeners();

        drawScene();
    }

    private void initListeners() {
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                solid.Solid active = scene.getActive();
                int key = e.getKeyCode();

                switch (key) {
                    case KeyEvent.VK_W -> camera = camera.forward(0.5);
                    case KeyEvent.VK_S -> camera = camera.backward(0.5);
                    case KeyEvent.VK_A -> camera = camera.left(0.5);
                    case KeyEvent.VK_D -> camera = camera.right(0.5);
                    case KeyEvent.VK_P -> perspective = !perspective;
                    case KeyEvent.VK_M -> {
                        wireframeOnly = !wireframeOnly;
                        renderer.setWireframeOnly(wireframeOnly);
                        e.consume();
                    }
                    // přepínání shaderu na aktivním tělese: texture -> interpolated -> constant -> zpět
                    case KeyEvent.VK_K -> {
                        Point3D lightPosition = new Point3D(0,0,0.5);
                        Vec3D normal = Pixel.getNormal().normalized().get();
                        lightPosition - pixel.getPosition(); // position musi byt ve world space
                        //Vec3d lightvecteor = (lightposioitn - pixel.getPositionWorldSpace().Normalize();

                        //spocitame uhel mezi normalou a light vektore
                        double lDot = Math.max(0, lightVector.dot(normal));

                        //todo vektor ke svetlu = pozice svetla 0 pozice vertexu (erex je raster)

                        return pixelCollor.mul(ambientColor.add(diffuseColor.Mul(lDotN)));

                        if (active != null) {
                            var current = active.getShader();
                            if (current instanceof ShaderTexture) {
                                active.setShader(new ShaderInterpolated());
                            } else if (current instanceof ShaderInterpolated) {
                                active.setShader(new ShaderConstant());
                            } else {
                                // výchozí: zkusíme texturový shader, pokud máme texturu
                                if (textureShader != null) {
                                    active.setShader(textureShader);
                                } else {
                                    active.setShader(new ShaderInterpolated());
                                }
                            }
                        }
                        e.consume();
                    }
                    case KeyEvent.VK_C -> {
                        if (active != null) {
                            int m = active.getColorBlendMode();
                            active.setColorBlendMode(m >= 3 ? 0 : m + 1);
                        }
                        e.consume();
                    }
                    case KeyEvent.VK_TAB -> {
                        if (e.isShiftDown()) scene.prevActive();
                        else scene.nextActive();
                        e.consume();
                    }
                    case KeyEvent.VK_1 -> { scene.setActiveIndex(0); e.consume(); }
                    case KeyEvent.VK_2 -> { scene.setActiveIndex(2); e.consume(); }
                    case KeyEvent.VK_3 -> { scene.setActiveIndex(3); e.consume(); }
                    case KeyEvent.VK_4 -> { scene.setActiveIndex(4); e.consume(); }
                    case KeyEvent.VK_5 -> { scene.setActiveIndex(5); e.consume(); }
                    case KeyEvent.VK_6 -> { scene.setActiveIndex(6); e.consume(); }
                    case KeyEvent.VK_7 -> { scene.setActiveIndex(6); e.consume(); }
                    case KeyEvent.VK_LEFT -> { applyTranslate(active, -MOVE_STEP, 0, 0); e.consume(); }
                    case KeyEvent.VK_RIGHT -> { applyTranslate(active, MOVE_STEP, 0, 0); e.consume(); }
                    case KeyEvent.VK_DOWN -> { applyTranslate(active, 0, -MOVE_STEP, 0); e.consume(); }
                    case KeyEvent.VK_UP -> { applyTranslate(active, 0, MOVE_STEP, 0); e.consume(); }
                    case KeyEvent.VK_PAGE_DOWN -> { applyTranslate(active, 0, 0, -MOVE_STEP); e.consume(); }
                    case KeyEvent.VK_PAGE_UP -> { applyTranslate(active, 0, 0, MOVE_STEP); e.consume(); }
                    case KeyEvent.VK_R -> { applyRotate(active, 'x', ROT_STEP); e.consume(); }
                    case KeyEvent.VK_F -> { applyRotate(active, 'x', -ROT_STEP); e.consume(); }
                    case KeyEvent.VK_T -> { applyRotate(active, 'y', ROT_STEP); e.consume(); }
                    case KeyEvent.VK_G -> { applyRotate(active, 'y', -ROT_STEP); e.consume(); }
                    case KeyEvent.VK_Y -> { applyRotate(active, 'z', ROT_STEP); e.consume(); }
                    case KeyEvent.VK_H -> { applyRotate(active, 'z', -ROT_STEP); e.consume(); }
                    case KeyEvent.VK_U -> { applyScale(active, SCALE_UP); e.consume(); }
                    case KeyEvent.VK_J -> { applyScale(active, SCALE_DOWN); e.consume(); }
                    default -> {
                        return;
                    }
                }

                drawScene();
            }
        });

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    lastMouseX = -1;
                    lastMouseY = -1;
                    drawScene();
                }
            }
        });

        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastMouseX < 0 || lastMouseY < 0) {
                    return;
                }
                int dx = e.getX() - lastMouseX;
                int dy = e.getY() - lastMouseY;
                lastMouseX = e.getX();
                lastMouseY = e.getY();

                camera = camera.addAzimuth(dx * MOUSE_SENSITIVITY);
                camera = camera.addZenith(-dy * MOUSE_SENSITIVITY);
                if (System.currentTimeMillis() - lastDrawTime >= REDRAW_THROTTLE_MS) {
                    drawScene();
                    lastDrawTime = System.currentTimeMillis();
                }
            }
        });

        panel.requestFocusInWindow();
    }

    private void applyTranslate(solid.Solid solid, double dx, double dy, double dz) {
        if (solid == null) return;
        solid.setModelMat(solid.getModelMat().mul(new Mat4Transl(dx, dy, dz)));
    }

    private void applyRotate(solid.Solid solid, char axis, double angle) {
        if (solid == null) return;
        Mat4 rot = switch (axis) {
            case 'x' -> new Mat4RotX(angle);
            case 'y' -> new Mat4RotY(angle);
            case 'z' -> new Mat4RotZ(angle);
            default -> null;
        };
        if (rot != null) solid.setModelMat(solid.getModelMat().mul(rot));
    }

    private void applyScale(solid.Solid solid, double factor) {
        if (solid == null) return;
        solid.setModelMat(solid.getModelMat().mul(new Mat4Scale(factor)));
    }

    private void drawScene() {
        panel.getRaster().clear();
        zBuffer.clear();

        // Pohledová transformace z kamery (WSAD už mění camera)
        Mat4 view = camera.getViewMatrix();

        // Projekce: perspektivní nebo ortogonální (klávesa P)
        int w = panel.getRaster().getWidth();
        int h = panel.getRaster().getHeight();
        double aspect = (double) h / w;
        double zn = 0.1;
        double zf = 100.0;

        Mat4 projection;
        if (perspective) {
            double fovY = Math.PI / 3; // 60 stupňů
            projection = new Mat4PerspRH(fovY, aspect, zn, zf);
        } else {
            double size = 8.0; // šířka/výška zobrazovacího objemu
            projection = new Mat4OrthoRH(size, size * aspect, zn, zf);
        }

        renderer.setViewMatrix(view);
        renderer.setProjectionMatrix(projection);
        renderer.setActiveSolid(scene.getActive());

        for (solid.Solid solid : scene.getSolids()) {
            renderer.render(solid);
        }

        lastDrawTime = System.currentTimeMillis();
        panel.repaint();
    }
}
