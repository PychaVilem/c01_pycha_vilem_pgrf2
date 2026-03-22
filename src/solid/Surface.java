package solid;

import model.Part;
import model.Topology;
import model.Vertex;
import transforms.Col;
import transforms.Mat4Identity;
import transforms.Vec2D;
import transforms.Vec3D;

import java.util.ArrayList;

// ctverec v rovine z=0 - test barev a textury, normala +z aby phong byl stejny na obou trojuhelnicich
public class Surface extends Solid {

    public Surface() {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        Vec3D n = new Vec3D(0, 0, 1);
        vertexBuffer.add(new Vertex(-0.5, -0.5, 0, new Col(0xff0000), new Vec2D(0, 0), n)); // 0
        vertexBuffer.add(new Vertex(0.5, -0.5, 0, new Col(0x00ff00), new Vec2D(1, 0), n));  // 1
        vertexBuffer.add(new Vertex(0.5, 0.5, 0, new Col(0x0000ff), new Vec2D(1, 1), n));   // 2
        vertexBuffer.add(new Vertex(-0.5, 0.5, 0, new Col(0xffff00), new Vec2D(0, 1), n));  // 3

        addIndices(0, 1, 1, 2, 2, 3, 3, 0);
        addIndices(0, 1, 2, 0, 2, 3);

        partBuffer.add(new Part(Topology.LINES, 4, 0));
        partBuffer.add(new Part(Topology.TRIANGLES, 2, 8));
    }
}

