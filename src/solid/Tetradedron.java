package solid;

import model.Part;
import model.Topology;
import model.Vertex;
import transforms.Col;
import transforms.Mat4Identity;

import java.util.ArrayList;

/**
 * Tetradedron (čtyřstěn) – 4 vrcholy, 6 hran, 4 trojúhelníkové stěny.
 */
public class Tetradedron extends Solid {

    public Tetradedron() {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        double a = 0.5;
        // Každý vrchol jiná barva → plynulý přechod na stěnách čtyřstěnu
        vertexBuffer.add(new Vertex(0, 0, 0, new Col(0xff0000)));           // 0 červená
        vertexBuffer.add(new Vertex(a * 2, 0, 0, new Col(0x00ff00)));       // 1 zelená
        vertexBuffer.add(new Vertex(a, a * 2, 0, new Col(0x0000ff)));       // 2 modrá
        vertexBuffer.add(new Vertex(a, a, a * 2, new Col(0xffff00)));       // 3 žlutá

        addIndices(0, 1, 1, 2, 2, 0, 0, 3, 1, 3, 2, 3);
        addIndices(0, 1, 2, 0, 1, 3, 1, 2, 3, 0, 2, 3);

        partBuffer.add(new Part(Topology.LINES, 6, 0));
        partBuffer.add(new Part(Topology.TRIANGLES, 4, 12));
    }
}

