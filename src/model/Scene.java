package model;

import solid.Solid;

import java.util.ArrayList;
import java.util.List;

public class Scene {

    private final List<Solid> solids;

    public Scene() {
        solids = new ArrayList<>();
        // přidáme základní šipku do scény
      //  solids.add(new Arrow());
    }

    public List<Solid> getSolids() {
        return solids;
    }
}
