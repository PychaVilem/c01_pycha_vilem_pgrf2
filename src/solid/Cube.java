package solid;

import model.Part;
import model.Topology;
import model.Vertex;
import transforms.Col;
import transforms.Mat4Identity;

import java.util.ArrayList;

/**
 * Kostka se středem v počátku, hrana délky 1.
 * Vertex/index/part buffer: 8 vrcholů, 12 hran (LINES), 12 trojúhelníků (TRIANGLES).
 */
public class Cube extends Solid {

    public Cube() {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        // Každý vrchol jiná barva → plynulý přechod na stěnách
        vertexBuffer.add(new Vertex(-0.5, -0.5, -0.5, new Col(0xff0000))); // 0 červená
        vertexBuffer.add(new Vertex(0.5, -0.5, -0.5, new Col(0x00ff00)));  // 1 zelená
        vertexBuffer.add(new Vertex(0.5, 0.5, -0.5, new Col(0x0000ff)));   // 2 modrá
        vertexBuffer.add(new Vertex(-0.5, 0.5, -0.5, new Col(0xffff00)));  // 3 žlutá
        vertexBuffer.add(new Vertex(-0.5, -0.5, 0.5, new Col(0xff00ff)));  // 4 magenta
        vertexBuffer.add(new Vertex(0.5, -0.5, 0.5, new Col(0x00ffff)));  // 5 cyan
        vertexBuffer.add(new Vertex(0.5, 0.5, 0.5, new Col(0xff8800)));   // 6 oranžová
        vertexBuffer.add(new Vertex(-0.5, 0.5, 0.5, new Col(0xffffff)));   // 7 bílá

        // Hrany (12 úseček) – zadní, přední, spojovací
        addIndices(0, 1, 1, 2, 2, 3, 3, 0);
        addIndices(4, 5, 5, 6, 6, 7, 7, 4);
        addIndices(0, 4, 1, 5, 2, 6, 3, 7);

        // Trojúhelníky (12) – 2 na stěnu
        addIndices(0, 1, 2, 0, 2, 3);   // zadní
        addIndices(5, 4, 7, 5, 7, 6);   // přední
        addIndices(1, 5, 6, 1, 6, 2);   // pravá
        addIndices(4, 0, 3, 4, 3, 7);   // levá
        addIndices(3, 2, 6, 3, 6, 7);   // horní
        addIndices(4, 5, 1, 4, 1, 0);   // dolní

        partBuffer.add(new Part(Topology.LINES, 12, 0));
        partBuffer.add(new Part(Topology.TRIANGLES, 12, 24));
    }
}
