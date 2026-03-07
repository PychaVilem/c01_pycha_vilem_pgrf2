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

        //todo az bude cely ceretezc nechci souradnice ve screen space
        vertexBuffer.add(new Vertex(200, 300, 0.5));
        vertexBuffer.add(new Vertex(250, 300, 0.5));
        vertexBuffer.add(new Vertex(250, 320, 0.5, new Col(0xff0000)));
        vertexBuffer.add(new Vertex(270, 300, 0.5));
        vertexBuffer.add(new Vertex(250, 250, 0.5));

        // Lines: segment 0-1 (1 úsečka, začíná na indexu 0)
        addIndices(0, 1);
        // Triangles: 4-3-2 (1 trojúhelník, začíná na indexu 2)
        addIndices(4, 3, 2);

        partBuffer.add(new Part(Topology.LINES, 1, 0));
        partBuffer.add(new Part(Topology.TRIANGLES, 1, 2));
    }

}
