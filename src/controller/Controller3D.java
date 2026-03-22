package controller;

import model.Scene;
import raster.ZBuffer;
import rasterize.LineRasterizer;
import rasterize.LineRasterizerGraphics;
import rasterize.TriangelRasterizer;
import renderer.RendererSolid;
import shader.Shader;
import shader.ShaderConstant;
import shader.ShaderInterpolated;
import shader.ShaderLit;
import shader.ShaderPhong;
import shader.ShaderTexture;
import solid.Arrow;
import solid.Axes;
import solid.Cone;
import solid.Cube;
import solid.Cylinder;
import solid.Solid;
import solid.Sphere;
import solid.Surface;
import solid.Tetradedron;
import transforms.*;
import view.Panel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

// hlavni ridici trida aplikace:
//  - drzi instanci Scene (seznam teles), Camera (pohled), RendererSolid + ZBuffer + rasterizery
//  - kazdy snimek: drawScene() -> nastavi view + projekci + svetlo -> renderer.render() na kazde teleso
//  - klavesnice/mys meni kameru nebo modelovou matici aktivniho telesa
//  - postranny panel: stejna logika jako klavesy, jen pres swing
public class Controller3D {
    private final Panel panel;
    private final ZBuffer zBuffer;
    private final TriangelRasterizer triangelRasterizer;
    private final RendererSolid renderer;
    private final Scene scene;
    private final LineRasterizer lineRasterizer;

    private Camera camera;
    private boolean perspective = true;
    private boolean wireframeOnly = false;

    // mys: odkud pocitam delta pri tahu
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private static final double MOUSE_SENSITIVITY = 0.005; // radiánů na pixel
    private static final double MOVE_STEP = 0.2;
    private static final double ROT_STEP = 0.15;
    private static final double SCALE_UP = 1.15;
    private static final double SCALE_DOWN = 1.0 / 1.15;
    private static final long REDRAW_THROTTLE_MS = 10;
    private long lastDrawTime = 0;
    // textury: kazdy typ pevneho telesa ma vlastni soubor (viz loadTextureShaders)
    // kdyz soubor chybi, promenna zustane null a textura pro ten typ nejde zapnout
    private ShaderTexture texCube;
    private ShaderTexture texSurface;
    private ShaderTexture texSphere;
    private ShaderTexture texTetrahedron;
    private ShaderTexture texCone;
    private ShaderTexture texCylinder;
    private ShaderTexture texArrow;
    private final ShaderInterpolated interpolatedShader = new ShaderInterpolated();

    private JTextArea infoArea;
    private JCheckBox cbPerspective;
    private JCheckBox cbWireframe;
    private JCheckBox cbLighting;
    private boolean lightingEnabled = true;
    private JComboBox<String> addTypeCombo;
    private JComboBox<ActiveItem> activeCombo;
    private int lastSolidListSize = -1;
    private Solid lastLightSourceForCombo;
    private boolean suppressUiCallbacks;

    // skryty radio - znamena "phong / interpolace" bez vlastnosti v panelu
    private JRadioButton rbShaderPhongHidden;
    private JRadioButton rbShaderConstant;
    private JRadioButton rbShaderTexture;
    private JRadioButton rbShaderInterpolated;
    private JPanel hslPanel;
    private JSlider hslH;
    private JSlider hslS;
    private JSlider hslL;
    private JPanel hslPreview;
    private boolean suppressHslCallbacks;

    // polozka v comboboxu aktivniho objektu
    private static final class ActiveItem {
        final int sceneIndex;
        private final String label;

        ActiveItem(int sceneIndex, String label) {
            this.sceneIndex = sceneIndex;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public Controller3D(Panel panel, JPanel sidePanel) {
        this.panel = panel;
        // zbuffer = barva + hloubka v jednom; triangel rasterizer do nej pise pres setPixelWithZTest
        this.zBuffer = new ZBuffer(panel.getRaster());
        this.lineRasterizer = new LineRasterizerGraphics(panel.getRaster());
        this.triangelRasterizer = new TriangelRasterizer(zBuffer);
        this.renderer = new RendererSolid(lineRasterizer, triangelRasterizer);
        this.scene = new Scene();
        loadTextureShaders();
        setupDefaultShaders();

        // kamera nastavena tak aby hledela na stred os (0,0,0.5)
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
        setupSidePanel(sidePanel);

        drawScene();
    }

    private void setupSidePanel(JPanel side) {
        if (side == null) {
            return;
        }
        side.removeAll();

        // --- textovy vystup stavu (aktivni objekt, svetlo, ...) obnovuje refreshSidebar() po kazdem drawScene
        JLabel title = new JLabel("Scéna");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        side.add(title);
        side.add(Box.createVerticalStrut(6));

        infoArea = new JTextArea(10, 22);
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane infoScroll = new JScrollPane(infoArea);
        infoScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(infoScroll);
        side.add(Box.createVerticalStrut(8));

        // --- stejne jako klavesy P / M / osvetleni - jen napojene na checkboxy
        cbPerspective = new JCheckBox("Perspektiva (P)");
        cbPerspective.setSelected(perspective);
        cbPerspective.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbPerspective.addActionListener(e -> {
            perspective = cbPerspective.isSelected();
            panel.requestFocusInWindow();
            drawScene();
        });
        side.add(cbPerspective);

        cbWireframe = new JCheckBox("Drátový model (M)");
        cbWireframe.setSelected(wireframeOnly);
        cbWireframe.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbWireframe.addActionListener(e -> {
            wireframeOnly = cbWireframe.isSelected();
            renderer.setWireframeOnly(wireframeOnly);
            panel.requestFocusInWindow();
            drawScene();
        });
        side.add(cbWireframe);

        cbLighting = new JCheckBox("Osvětlení (Phong)");
        cbLighting.setSelected(lightingEnabled);
        cbLighting.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbLighting.setToolTipText("Vypne bodové světlo – jen základní barva (interpolace / textura / konstanta).");
        cbLighting.addActionListener(e -> {
            lightingEnabled = cbLighting.isSelected();
            panel.requestFocusInWindow();
            drawScene();
        });
        side.add(cbLighting);
        side.add(Box.createVerticalStrut(8));

        // --- svetlo: pozice = getTranslate() na vybranem solidu (viz getLightPositionWorld)
        JLabel lightSrcLabel = new JLabel("Zdroj světla (Phong):");
        lightSrcLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(lightSrcLabel);

        JButton btnLightFromActive = new JButton("Aktivní těleso = zdroj světla");
        btnLightFromActive.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLightFromActive.setMaximumSize(new Dimension(Integer.MAX_VALUE, btnLightFromActive.getPreferredSize().height));
        btnLightFromActive.setToolTipText("Pozice a barva bodového světla se vezmou z aktivního objektu (střed transformace).");
        btnLightFromActive.addActionListener(e -> {
            Solid a = scene.getActive();
            if (a != null && !(a instanceof Axes)) {
                scene.setLightSourceSolid(a);
            }
            panel.requestFocusInWindow();
            drawScene();
        });
        side.add(btnLightFromActive);

        JButton btnLightDefault = new JButton("Zpět na světelnou kouli");
        btnLightDefault.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLightDefault.setMaximumSize(new Dimension(Integer.MAX_VALUE, btnLightDefault.getPreferredSize().height));
        btnLightDefault.addActionListener(e -> {
            scene.resetLightSourceToDefault();
            panel.requestFocusInWindow();
            drawScene();
        });
        side.add(btnLightDefault);
        side.add(Box.createVerticalStrut(8));

        // --- shader se meni jen na aktivnim telese; vzdycky to obalim do ShaderLit(...) aby slo phong
        // rbShaderPhongHidden = vychozi stav "jen interpolace pod phongem" kdyz nic nevyberu v panelu
        JLabel shaderLabel = new JLabel("Shader (aktivní objekt):");
        shaderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(shaderLabel);

        rbShaderPhongHidden = new JRadioButton();
        rbShaderConstant = new JRadioButton("Konstantní barva");
        rbShaderTexture = new JRadioButton("Textura");
        rbShaderInterpolated = new JRadioButton("Interpolovaná barva");
        rbShaderConstant.setAlignmentX(Component.LEFT_ALIGNMENT);
        rbShaderTexture.setAlignmentX(Component.LEFT_ALIGNMENT);
        rbShaderInterpolated.setAlignmentX(Component.LEFT_ALIGNMENT);

        ButtonGroup shaderGroup = new ButtonGroup();
        shaderGroup.add(rbShaderPhongHidden);
        shaderGroup.add(rbShaderConstant);
        shaderGroup.add(rbShaderTexture);
        shaderGroup.add(rbShaderInterpolated);
        rbShaderPhongHidden.setSelected(true);

        java.awt.event.ActionListener shaderRadioListener = e -> {
            if (suppressUiCallbacks) return;
            applyShaderFromSelectedRadios();
            panel.requestFocusInWindow();
            drawScene();
        };
        rbShaderConstant.addActionListener(shaderRadioListener);
        rbShaderTexture.addActionListener(shaderRadioListener);
        rbShaderInterpolated.addActionListener(shaderRadioListener);

        side.add(rbShaderConstant);
        side.add(rbShaderTexture);
        side.add(rbShaderInterpolated);

        hslPanel = new JPanel();
        hslPanel.setLayout(new BoxLayout(hslPanel, BoxLayout.Y_AXIS));
        hslPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hslPanel.setBorder(BorderFactory.createTitledBorder("HSL barvy"));

        hslPreview = new JPanel();
        hslPreview.setPreferredSize(new Dimension(200, 26));
        hslPreview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        hslPreview.setOpaque(true);
        hslPreview.setBackground(Color.RED);
        hslPreview.setAlignmentX(Component.LEFT_ALIGNMENT);
        hslPanel.add(hslPreview);
        hslPanel.add(Box.createVerticalStrut(4));

        hslH = new JSlider(0, 360, 0);
        hslS = new JSlider(0, 100, 100);
        hslL = new JSlider(0, 100, 50);
        for (JSlider sl : new JSlider[]{hslH, hslS, hslL}) {
            sl.setAlignmentX(Component.LEFT_ALIGNMENT);
            sl.setMaximumSize(new Dimension(Integer.MAX_VALUE, sl.getPreferredSize().height));
        }
        hslH.setMajorTickSpacing(120);
        hslH.setMinorTickSpacing(30);
        hslH.setPaintTicks(true);
        hslS.setMajorTickSpacing(25);
        hslS.setPaintTicks(true);
        hslL.setMajorTickSpacing(25);
        hslL.setPaintTicks(true);

        JLabel lh = new JLabel("Odstín (H°)");
        lh.setAlignmentX(Component.LEFT_ALIGNMENT);
        hslPanel.add(lh);
        hslPanel.add(hslH);
        JLabel ls = new JLabel("Sytost (S %)");
        ls.setAlignmentX(Component.LEFT_ALIGNMENT);
        hslPanel.add(ls);
        hslPanel.add(hslS);
        JLabel ll = new JLabel("Světlost (L %)");
        ll.setAlignmentX(Component.LEFT_ALIGNMENT);
        hslPanel.add(ll);
        hslPanel.add(hslL);

        ChangeListener hslListener = e -> onHslSlidersChanged();
        hslH.addChangeListener(hslListener);
        hslS.addChangeListener(hslListener);
        hslL.addChangeListener(hslListener);

        hslPanel.setVisible(false);
        side.add(hslPanel);
        side.add(Box.createVerticalStrut(8));

        // --- dynamicke pridani telesa pred svetelnou kouli (viz Scene.addSolidBeforeLight)
        JLabel addLabel = new JLabel("Přidat těleso:");
        addLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(addLabel);
        addTypeCombo = new JComboBox<>(new String[]{
                "Cube", "Cylinder", "Cone", "Sphere", "Tetrahedron", "Surface", "Arrow"
        });
        addTypeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        addTypeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, addTypeCombo.getPreferredSize().height));
        side.add(addTypeCombo);

        JButton btnAdd = new JButton("Přidat");
        btnAdd.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnAdd.addActionListener(e -> {
            String type = (String) addTypeCombo.getSelectedItem();
            if (type != null) {
                addSolidFromType(type);
            }
            panel.requestFocusInWindow();
            drawScene();
        });
        side.add(btnAdd);
        side.add(Box.createVerticalStrut(8));

        // --- vyber aktivniho: musi sedet s scene.getActiveIndex(); suppressUiCallbacks aby se necyklilo pri programove zmene
        JLabel activeLabel = new JLabel("Aktivní objekt:");
        activeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(activeLabel);
        activeCombo = new JComboBox<>();
        activeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        activeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, activeCombo.getPreferredSize().height));
        activeCombo.addActionListener(e -> {
            if (suppressUiCallbacks) return;
            ActiveItem it = (ActiveItem) activeCombo.getSelectedItem();
            if (it != null) {
                scene.setActiveIndex(it.sceneIndex);
                panel.requestFocusInWindow();
                drawScene();
            }
        });
        side.add(activeCombo);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        nav.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton btnPrev = new JButton("←");
        JButton btnNext = new JButton("→");
        btnPrev.addActionListener(e -> {
            scene.prevActive();
            panel.requestFocusInWindow();
            drawScene();
        });
        btnNext.addActionListener(e -> {
            scene.nextActive();
            panel.requestFocusInWindow();
            drawScene();
        });
        nav.add(btnPrev);
        nav.add(btnNext);
        side.add(nav);

        side.add(Box.createVerticalGlue());

        if (!hasAnyTextureShader()) {
            rbShaderTexture.setEnabled(false);
        }
    }

    private static Color awtFromCol(Col col) {
        return new Color(col.getRGB());
    }

    // --- HSL pomocne: konstantni barva jde nastavit slidery; prevod rgb<->hsl je obycejna matematika
    private Col colFromSliders() {
        return hslToCol(hslH.getValue(), hslS.getValue() / 100.0, hslL.getValue() / 100.0);
    }

    private static Col hslToCol(double hDeg, double s, double l) {
        double h = (hDeg / 360.0) % 1.0;
        if (h < 0) h += 1.0;
        double r;
        double g;
        double b;
        if (s < 1e-8) {
            r = g = b = l;
        } else {
            double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;
            r = hueToRgb(p, q, h + 1.0 / 3);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1.0 / 3);
        }
        return new Col(r, g, b);
    }

    private static double hueToRgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0 / 6) return p + (q - p) * 6 * t;
        if (t < 0.5) return q;
        if (t < 2.0 / 3) return p + (q - p) * (2.0 / 3 - t) * 6;
        return p;
    }

    private static void colToHslSliders(Col c, int[] hslOut) {
        double r = c.getR();
        double g = c.getG();
        double b = c.getB();
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double h;
        double s;
        double l = (max + min) / 2;
        if (max - min < 1e-10) {
            h = 0;
            s = 0;
        } else {
            double d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            if (max == r) {
                h = ((g - b) / d + (g < b ? 6 : 0)) / 6;
            } else if (max == g) {
                h = ((b - r) / d + 2) / 6;
            } else {
                h = ((r - g) / d + 4) / 6;
            }
        }
        hslOut[0] = (int) Math.round(h * 360) % 360;
        if (hslOut[0] < 0) hslOut[0] += 360;
        hslOut[1] = clampInt((int) Math.round(s * 100), 0, 100);
        hslOut[2] = clampInt((int) Math.round(l * 100), 0, 100);
    }

    private static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void setSlidersFromCol(Col col) {
        if (hslH == null) return;
        suppressHslCallbacks = true;
        try {
            int[] hsl = new int[3];
            colToHslSliders(col, hsl);
            hslH.setValue(hsl[0]);
            hslS.setValue(hsl[1]);
            hslL.setValue(hsl[2]);
            hslPreview.setBackground(awtFromCol(col));
        } finally {
            suppressHslCallbacks = false;
        }
    }

    private void onHslSlidersChanged() {
        if (suppressHslCallbacks || hslH == null) return;
        if (!rbShaderConstant.isSelected()) return;
        Solid active = scene.getActive();
        if (active == null || active instanceof Axes) return;

        Col col = colFromSliders();
        Shader base = baseShader(active.getShader());
        if (base instanceof ShaderConstant sc) {
            sc.setSolidColor(col);
        } else {
            active.setShader(new ShaderLit(new ShaderConstant(col)));
        }
        hslPreview.setBackground(awtFromCol(col));
        panel.requestFocusInWindow();
        drawScene();
    }

    private void updateHslPanelVisibility() {
        if (hslPanel == null) return;
        boolean show = rbShaderConstant != null && rbShaderConstant.isSelected();
        hslPanel.setVisible(show);
        Container p = hslPanel.getParent();
        if (p != null) {
            p.revalidate();
        }
    }

    // vola se pri kliku na radio - nastavi aktivnimu telesovy novy ShaderLit(vnitrek)
    private void applyShaderFromSelectedRadios() {
        Solid active = scene.getActive();
        if (active == null) return;
        if (active instanceof Axes) return;

        if (rbShaderInterpolated.isSelected()) {
            active.setShader(new ShaderLit(interpolatedShader));
        } else if (rbShaderTexture.isSelected()) {
            ShaderTexture ts = textureShaderForSolid(active);
            if (ts != null) {
                active.setShader(new ShaderLit(ts));
            }
        } else if (rbShaderConstant.isSelected()) {
            Col c = colFromSliders();
            if (baseShader(active.getShader()) instanceof ShaderConstant sc) {
                sc.setSolidColor(c);
            } else {
                active.setShader(new ShaderLit(new ShaderConstant(c)));
            }
            hslPreview.setBackground(awtFromCol(c));
        }
        updateHslPanelVisibility();
    }

    // po zmene aktivniho objektu nastavim radio podle toho jaky ma vnitrni shader (baseShader)
    private void syncShaderRadiosFromActive() {
        if (rbShaderConstant == null) return;
        Solid active = scene.getActive();
        if (active == null) {
            rbShaderPhongHidden.setSelected(true);
            updateHslPanelVisibility();
            return;
        }
        Shader sh = active.getShader();
        Shader b = baseShader(sh);
        if (b instanceof ShaderTexture) {
            if (textureShaderForSolid(active) != null) {
                rbShaderTexture.setSelected(true);
            } else {
                rbShaderPhongHidden.setSelected(true);
            }
        } else if (b instanceof ShaderInterpolated) {
            rbShaderInterpolated.setSelected(true);
        } else if (b instanceof ShaderConstant sc) {
            rbShaderConstant.setSelected(true);
            setSlidersFromCol(sc.getSolidColor());
        } else {
            rbShaderPhongHidden.setSelected(true);
        }
        rbShaderTexture.setEnabled(textureShaderForSolid(active) != null);
        updateHslPanelVisibility();
    }

    private Solid createSolidByName(String name) {
        return switch (name) {
            case "Cube" -> new Cube();
            case "Cylinder" -> new Cylinder();
            case "Cone" -> new Cone();
            case "Sphere" -> new Sphere();
            case "Tetrahedron" -> new Tetradedron();
            case "Surface" -> new Surface();
            case "Arrow" -> new Arrow();
            default -> null;
        };
    }

    private void addSolidFromType(String type) {
        Solid s = createSolidByName(type);
        if (s == null) return;
        s.setModelMat(new Mat4Transl(0, 0, 0.5));
        scene.addSolidBeforeLight(s);
        applyShaderForNewSolid(s);
    }

    private void applyShaderForNewSolid(Solid solid) {
        if (solid instanceof Axes) return;
        solid.setShader(new ShaderLit(interpolatedShader));
    }

    // klavesa K: textura -> interpolace -> konstanta -> zpet na texturu (kdyz existuje soubor)
    private void cycleShaderOnActive() {
        Solid active = scene.getActive();
        if (active == null) return;
        Shader base = baseShader(active.getShader());
        if (base instanceof ShaderTexture) {
            active.setShader(new ShaderLit(interpolatedShader));
        } else if (base instanceof ShaderInterpolated) {
            active.setShader(new ShaderLit(new ShaderConstant()));
        } else if (base instanceof ShaderConstant) {
            ShaderTexture ts = textureShaderForSolid(active);
            if (ts != null) {
                active.setShader(new ShaderLit(ts));
            } else {
                active.setShader(new ShaderLit(interpolatedShader));
            }
        } else {
            active.setShader(new ShaderLit(interpolatedShader));
        }
    }

    // vyndam ShaderLit zvenci kdyz potrebuju vedet jestli je to textura nebo konstanta
    private static Shader baseShader(Shader sh) {
        return sh instanceof ShaderLit lit ? lit.getBase() : sh;
    }

    private String buildInfoText() {
        Solid active = scene.getActive();
        String activeName = active == null ? "—" : active.getClass().getSimpleName();
        Point3D lp = getLightPositionWorld();
        Solid lightSrc = scene.getLightSourceSolid();
        String lightSrcName = lightSrc == null ? "—" : lightSrc.getClass().getSimpleName();
        if (lightSrc == scene.getLightSphere()) {
            lightSrcName = "světelná koule";
        }
        return String.format(
                "Aktivní: %s%nIndex: %d / %d%nPerspektiva: %s%nDrát: %s%nZdroj světla: %s%nPozice světla: (%.2f, %.2f, %.2f)%n%nKlávesy: WSAD pohled, myš otáčení, šipky&PgUp/PgDown posun, RFTGYH rotace, UJ měřítko",
                activeName,
                scene.getActiveIndex(),
                Math.max(0, scene.getSolids().size() - 1),
                perspective ? "ano" : "ne",
                wireframeOnly ? "ano" : "ne",
                lightSrcName,
                lp.getX(), lp.getY(), lp.getZ()
        );
    }

    private void rebuildActiveCombo() {
        activeCombo.removeAllItems();
        List<Solid> list = scene.getSolids();
        for (int i = 0; i < list.size(); i++) {
            if (i == Scene.AXES_INDEX) continue;
            Solid s = list.get(i);
            String base = s == scene.getLightSphere() ? "světelná koule" : s.getClass().getSimpleName();
            if (s == scene.getLightSourceSolid()) {
                base += " [světlo]";
            }
            activeCombo.addItem(new ActiveItem(i, i + ": " + base));
        }
        lastSolidListSize = list.size();
        lastLightSourceForCombo = scene.getLightSourceSolid();
    }

    private void syncActiveComboSelection() {
        int want = scene.getActiveIndex();
        for (int i = 0; i < activeCombo.getItemCount(); i++) {
            ActiveItem it = activeCombo.getItemAt(i);
            if (it.sceneIndex == want) {
                activeCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void refreshSidebar() {
        if (infoArea == null) return;
        suppressUiCallbacks = true;
        try {
            infoArea.setText(buildInfoText());
            cbPerspective.setSelected(perspective);
            cbWireframe.setSelected(wireframeOnly);
            if (cbLighting != null) {
                cbLighting.setSelected(lightingEnabled);
            }
            if (scene.getSolids().size() != lastSolidListSize
                    || scene.getLightSourceSolid() != lastLightSourceForCombo) {
                rebuildActiveCombo();
            }
            syncActiveComboSelection();
            syncShaderRadiosFromActive();
        } finally {
            suppressUiCallbacks = false;
        }
    }

    private void initListeners() {
        panel.setFocusTraversalKeysEnabled(false);

        // klavesy (shrnuti):
        //  WSAD = pohyb kamery, mys drag = azimut/zenit
        //  P = perspektiva/orto, M = drat, K = cyklus shaderu na aktivnim
        //  sipky + pgup/pgdn = posun aktivniho telesa, RFTGYH = rotace kolem os, UJ = meritko
        //  tab/shift+tab = dalsi/predchozi aktivni (osy preskocene v Scene)
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Solid active = scene.getActive();
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
                    case KeyEvent.VK_K -> {
                        cycleShaderOnActive();
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
                    case KeyEvent.VK_7 -> { scene.setActiveIndex(7); e.consume(); }
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
                panel.requestFocusInWindow();
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
                // throttling: pri rychlem tahu mysi neprekreslovat kazdy pixel jinak to zbytecne zatezuje cpu
                if (System.currentTimeMillis() - lastDrawTime >= REDRAW_THROTTLE_MS) {
                    drawScene();
                    lastDrawTime = System.currentTimeMillis();
                }
            }
        });

        panel.requestFocusInWindow();
    }

    // modelove transformace aktivniho telesa (nezasahuje do kamery):
    //  posun = novy translacni krat stara matice zprava
    //  rotace = rotace * stara matice (otacim ve svete kolem pocatku - jak to delame na cviceni)
    //  meritko = scale * stara matice
    private void applyTranslate(Solid solid, double dx, double dy, double dz) {
        if (solid == null) return;
        solid.setModelMat(solid.getModelMat().mul(new Mat4Transl(dx, dy, dz)));
    }

    private void applyRotate(Solid solid, char axis, double angle) {
        if (solid == null) return;
        Mat4 rot = switch (axis) {
            case 'x' -> new Mat4RotX(angle);
            case 'y' -> new Mat4RotY(angle);
            case 'z' -> new Mat4RotZ(angle);
            default -> null;
        };
        if (rot != null) solid.setModelMat(rot.mul(solid.getModelMat()));
    }

    private void applyScale(Solid solid, double factor) {
        if (solid == null) return;
        solid.setModelMat(new Mat4Scale(factor).mul(solid.getModelMat()));
    }

    // jeden snimek: vymazu barvu i hloubku, slozim view a projekci, predam svetlo do rendereru, nakreslim vsechny solidy
    private void drawScene() {
        panel.getRaster().clear();
        zBuffer.clear();

        Mat4 view = camera.getViewMatrix();

        int w = panel.getRaster().getWidth();
        int h = panel.getRaster().getHeight();
        double aspect = (double) h / w;
        double zn = 0.1;
        double zf = 100.0;

        Mat4 projection;
        if (perspective) {
            double fovY = Math.PI / 3;
            projection = new Mat4PerspRH(fovY, aspect, zn, zf);
        } else {
            double size = 8.0;
            projection = new Mat4OrthoRH(size, size * aspect, zn, zf);
        }

        // kdyz vypnu, ShaderLit vraci jen "zakladni" barvu bez vypoctu svetla (ukazka materialu)
        renderer.setPhongLightingEnabled(lightingEnabled);

        renderer.setViewMatrix(view);
        renderer.setProjectionMatrix(projection);
        renderer.setCameraPositionWorld(camera.getPosition());
        renderer.setLightPositionWorld(getLightPositionWorld());
        renderer.setLightColorWorld(getLightColorWorld());
        renderer.setActiveSolid(scene.getActive());

        for (Solid solid : scene.getSolids()) {
            renderer.render(solid);
        }

        lastDrawTime = System.currentTimeMillis();
        panel.repaint();
        refreshSidebar();
    }

    private Point3D getLightPositionWorld() {
        Solid src = scene.getLightSourceSolid();
        Vec3D t = src.getModelMat().getTranslate();
        return new Point3D(t.getX(), t.getY(), t.getZ());
    }

    // barva svetla pro phong: u konstantniho shaderu vezmu tu barvu, jinak prvni vrchol (typicky cela koule ma stejne)
    private Col getLightColorWorld() {
        Solid ls = scene.getLightSourceSolid();
        Shader sh = ls.getShader();
        if (baseShader(sh) instanceof ShaderConstant sc) {
            return sc.getSolidColor();
        }
        if (sh instanceof ShaderPhong) {
            return new Col(1.0, 1.0, 0.95);
        }
        if (!ls.getVertexBuffer().isEmpty()) {
            return new Col(ls.getVertexBuffer().get(0).getColor());
        }
        return new Col(0xffffff);
    }

    private static BufferedImage loadTextureImage(String... fileNames) {
        for (String name : fileNames) {
            File f = new File("resources/textures/" + name);
            if (f.exists()) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) {
                        return img;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static ShaderTexture shaderTextureFromImage(BufferedImage img) {
        return img == null ? null : new ShaderTexture(img);
    }

    // pri startu controlleru: nacist vsechny jpg z resources/textures (kdyz chybi, tex zustane null)
    private void loadTextureShaders() {
        texCube = shaderTextureFromImage(loadTextureImage("texture3.jpg", "texture3.JPG"));
        texSurface = shaderTextureFromImage(loadTextureImage("texture2.jpg"));
        texSphere = shaderTextureFromImage(loadTextureImage("texture4.jpg"));
        texTetrahedron = shaderTextureFromImage(loadTextureImage("texture5.jpg"));
        texCone = shaderTextureFromImage(loadTextureImage("texture6.jpg"));
        texCylinder = shaderTextureFromImage(loadTextureImage("texture7.jpg"));
        texArrow = shaderTextureFromImage(loadTextureImage("texture2.jpg"));
    }

    // kterou texturu pouzit podle typu telesa (zadani: kazde teleso jina)
    private ShaderTexture textureShaderForSolid(Solid s) {
        if (s == scene.getLightSphere()) {
            return null;
        }
        if (s instanceof Cube) return texCube;
        if (s instanceof Surface) return texSurface;
        if (s instanceof Sphere) return texSphere;
        if (s instanceof Tetradedron) return texTetrahedron;
        if (s instanceof Cone) return texCone;
        if (s instanceof Cylinder) return texCylinder;
        if (s instanceof Arrow) return texArrow;
        return null;
    }

    private boolean hasAnyTextureShader() {
        return texCube != null || texSurface != null || texSphere != null || texTetrahedron != null
                || texCone != null || texCylinder != null || texArrow != null;
    }

    // start sceny: vsem telesum krome os dam ShaderLit(interpolovane barvy)
    private void setupDefaultShaders() {
        for (Solid solid : scene.getSolids()) {
            if (solid instanceof Axes) {
                continue;
            }
            solid.setShader(new ShaderLit(interpolatedShader));
        }
    }
}
