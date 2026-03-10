package model;

import solid.Arrow;
import solid.Axes;
import solid.Cone;
import solid.Cube;
import solid.Cylinder;
import solid.Solid;
import solid.Surface;
import solid.Tetradedron;
import transforms.Mat4Transl;

import java.util.ArrayList;
import java.util.List;

public class Scene {

    private final List<Solid> solids;
    private int activeIndex;

    /** Index souřadnicových os – nelze vybrat jako aktivní těleso. */
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
        activeIndex = 0;
    }

    public List<Solid> getSolids() {
        return solids;
    }

    /** Index aktuálně vybraného tělesa (pro modelovací transformace). */
    public int getActiveIndex() {
        return activeIndex;
    }

    /** Aktuálně vybrané těleso, nebo null pokud je scéna prázdná. */
    public Solid getActive() {
        if (solids.isEmpty()) return null;
        return solids.get(activeIndex);
    }

    /** Přepne na další těleso (cyklicky). Přeskočí osy XYZ. */
    public void nextActive() {
        if (solids.isEmpty()) return;
        do {
            activeIndex = (activeIndex + 1) % solids.size();
        } while (activeIndex == AXES_INDEX);
    }

    /** Přepne na předchozí těleso (cyklicky). Přeskočí osy XYZ. */
    public void prevActive() {
        if (solids.isEmpty()) return;
        do {
            activeIndex = (activeIndex - 1 + solids.size()) % solids.size();
        } while (activeIndex == AXES_INDEX);
    }

    /** Nastaví aktivní těleso podle indexu. Osy (index 1) nelze vybrat – nastaví se 0. */
    public void setActiveIndex(int index) {
        if (index < 0 || index >= solids.size()) return;
        if (index == AXES_INDEX) {
            activeIndex = 0;
        } else {
            activeIndex = index;
        }
    }
}
