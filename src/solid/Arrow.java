package solid;

import model.Part;
import model.Topology;
import model.Vertex;
import transforms.Col;
import transforms.Mat4Identity;

import java.util.ArrayList;
import java.util.Arrays;

public class Arrow extends Solid {

    public Arrow() {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        // šipka v malém 3D prostoru kolem počátku (z ≈ 0.5)
        // tělo šipky
        vertexBuffer.add(new Vertex(-0.5, 0.0, 0.5));          // 0
        vertexBuffer.add(new Vertex(0.0, 0.0, 0.5));           // 1
        // hrot šipky
        vertexBuffer.add(new Vertex(0.2, 0.15, 0.5, new Col(0xff0000))); // 2
        vertexBuffer.add(new Vertex(0.4, 0.0, 0.5, new Col(0xff0000)));  // 3
        vertexBuffer.add(new Vertex(0.2, -0.15, 0.5, new Col(0xff0000)));// 4

        // Lines: segment 0-1 (1 úsečka, začíná na indexu 0)
        addIndices(0, 1);
        // Triangles: 1-2-4 (1 trojúhelník, začíná na indexu 2)
        addIndices(1, 2, 4);

        partBuffer.add(new Part(Topology.LINES, 1, 0));
        partBuffer.add(new Part(Topology.TRIANGLES, 1, 2));
    }

}
