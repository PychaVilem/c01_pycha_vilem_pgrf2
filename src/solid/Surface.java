package solid;

import model.Part;
import model.Topology;
import model.Vertex;
import transforms.Col;
import transforms.Mat4Identity;

import java.util.ArrayList;

/**
 * Rovina v XY (z=0), čtverec od -0.5 do 0.5 v x i y.
 * 4 vrcholy, 4 hrany, 2 trojúhelníky.
 */
public class Surface extends Solid {

    public Surface() {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        // Každý roh jiná barva → plynulý přechod díky interpolaci v rasterizéru
        vertexBuffer.add(new Vertex(-0.5, -0.5, 0, new Col(0xff0000))); // 0 červená
        vertexBuffer.add(new Vertex(0.5, -0.5, 0, new Col(0x00ff00)));  // 1 zelená
        vertexBuffer.add(new Vertex(0.5, 0.5, 0, new Col(0x0000ff)));   // 2 modrá
        vertexBuffer.add(new Vertex(-0.5, 0.5, 0, new Col(0xffff00)));  // 3 žlutá

        addIndices(0, 1, 1, 2, 2, 3, 3, 0);
        addIndices(0, 1, 2, 0, 2, 3);

        partBuffer.add(new Part(Topology.LINES, 4, 0));
        partBuffer.add(new Part(Topology.TRIANGLES, 2, 8));
    }
}

