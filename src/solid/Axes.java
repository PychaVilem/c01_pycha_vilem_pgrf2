package solid;

import model.Part;
import model.Topology;
import model.Vertex;
import transforms.Col;
import transforms.Mat4Identity;

import java.util.ArrayList;

public class Axes extends Solid {

    public Axes() {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        // X osa - červená šipka podél +X
        int baseX = vertexBuffer.size();
        vertexBuffer.add(new Vertex(0, 0, 0.5, new Col(0xff0000)));   // 0
        vertexBuffer.add(new Vertex(1, 0, 0.5, new Col(0xff0000)));   // 1
        vertexBuffer.add(new Vertex(0.8, 0.1, 0.5, new Col(0xff0000)));// 2
        vertexBuffer.add(new Vertex(0.8, -0.1, 0.5, new Col(0xff0000)));// 3

        // Y osa - zelená šipka podél +Y
        int baseY = vertexBuffer.size();
        vertexBuffer.add(new Vertex(0, 0, 0.5, new Col(0x00ff00)));   // 4
        vertexBuffer.add(new Vertex(0, 1, 0.5, new Col(0x00ff00)));   // 5
        vertexBuffer.add(new Vertex(0.1, 0.8, 0.5, new Col(0x00ff00)));// 6
        vertexBuffer.add(new Vertex(-0.1, 0.8, 0.5, new Col(0x00ff00)));// 7

        // Z osa - modrá šipka podél +Z
        int baseZ = vertexBuffer.size();
        vertexBuffer.add(new Vertex(0, 0, 0.5, new Col(0x0000ff)));   // 8
        vertexBuffer.add(new Vertex(0, 0, 1.5, new Col(0x0000ff)));   // 9
        vertexBuffer.add(new Vertex(0.1, 0, 1.3, new Col(0x0000ff))); // 10
        vertexBuffer.add(new Vertex(-0.1, 0, 1.3, new Col(0x0000ff)));// 11

        // indexy pro hrany (3 úsečky)
        addIndices(
                baseX + 0, baseX + 1,
                baseY + 0, baseY + 1,
                baseZ + 0, baseZ + 1
        );

        // indexy pro hroty (3 trojúhelníky)
        addIndices(
                baseX + 1, baseX + 2, baseX + 3,
                baseY + 1, baseY + 2, baseY + 3,
                baseZ + 1, baseZ + 2, baseZ + 3
        );

        // 3 úsečky od indexu 0
        partBuffer.add(new Part(Topology.LINES, 3, 0));
        // 3 trojúhelníky od indexu 6
        partBuffer.add(new Part(Topology.TRIANGLES, 3, 6));
    }
}

