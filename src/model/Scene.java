package model;

import solid.Arrow;
import solid.Axes;
import solid.Cone;
import solid.Cube;
import solid.Cylinder;
import solid.Solid;
import solid.Sphere;
import solid.Surface;
import solid.Tetradedron;
import transforms.Mat4Transl;

import java.util.ArrayList;
import java.util.List;

// seznam vsech teles ve scene + ktery objekt je aktivni (na nej jdou transformace)
// a ktery objekt urcuje pozici svetla pro osvetleni
public class Scene {

    private final List<Solid> solids;
    private final Sphere lightSphere;
    // odkud beru pozici (a barvu) bodoveho svetla - obvykle mala koule na konci seznamu
    private Solid lightSourceSolid;
    private int activeIndex;

    // osy jsou na indexu 1, ty nechci dat jako aktivni pri tabu
    public static final int AXES_INDEX = 1;

    public Scene() {
        solids = new ArrayList<>();
        solids.add(new Arrow());
        solids.add(new Axes());
        Solid cube = new Cube();
        cube.setModelMat(new Mat4Transl(2, 0, 0.5));
        solids.add(cube);
        Solid surface = new Surface();
        surface.setModelMat(new Mat4Transl(-1.5, 0, 0.5));
        solids.add(surface);
        Solid cylinder = new Cylinder();
        cylinder.setModelMat(new Mat4Transl(0, -1.5, 0.5));
        solids.add(cylinder);
        Solid tetradedron = new Tetradedron();
        tetradedron.setModelMat(new Mat4Transl(0, 1.5, 0.5));
        solids.add(tetradedron);
        Solid cone = new Cone();
        cone.setModelMat(new Mat4Transl(-2, -1, 0.5));
        solids.add(cone);
        Solid sphere = new Sphere();
        sphere.setModelMat(new Mat4Transl(1.5, 1.3, 0.7));
        solids.add(sphere);
        // mala bila koule jako vizual zdroje svetla (posledni v seznamu)
        lightSphere = new Sphere(0.18, 12, 18, new transforms.Col(0xffffff));
        lightSphere.setModelMat(new Mat4Transl(0.0, 0.0, 1.2));
        solids.add(lightSphere);
        lightSourceSolid = lightSphere;
        activeIndex = 0;
    }

    public List<Solid> getSolids() {
        return solids;
    }

    public Sphere getLightSphere() {
        return lightSphere;
    }

    public Solid getLightSourceSolid() {
        return lightSourceSolid;
    }

    // svetlo = stred modelove matice vybraneho telesa (osy ne)
    public void setLightSourceSolid(Solid solid) {
        if (solid == null) return;
        if (solid instanceof Axes) return;
        lightSourceSolid = solid;
    }

    public void resetLightSourceToDefault() {
        lightSourceSolid = lightSphere;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public Solid getActive() {
        if (solids.isEmpty()) return null;
        return solids.get(activeIndex);
    }

    public void nextActive() {
        if (solids.isEmpty()) return;
        do {
            activeIndex = (activeIndex + 1) % solids.size();
        } while (activeIndex == AXES_INDEX);
    }

    public void prevActive() {
        if (solids.isEmpty()) return;
        do {
            activeIndex = (activeIndex - 1 + solids.size()) % solids.size();
        } while (activeIndex == AXES_INDEX);
    }

    public void setActiveIndex(int index) {
        if (index < 0 || index >= solids.size()) return;
        if (index == AXES_INDEX) {
            activeIndex = 0;
        } else {
            activeIndex = index;
        }
    }

    // nova vec vzdy pred svetelnou kouli (ta zustane posledni)
    public void addSolidBeforeLight(Solid solid) {
        if (solids.isEmpty()) {
            solids.add(solid);
            activeIndex = 0;
            return;
        }
        int insertAt = solids.size() - 1;
        solids.add(insertAt, solid);
        activeIndex = insertAt;
    }
}
