package solid;

import model.Part;
import model.Topology;
import model.Vertex;
import transforms.Col;
import transforms.Mat4Identity;
import transforms.Vec2D;
import transforms.Vec3D;

import java.util.ArrayList;

// mala sipka u pocatku - ukazka usecky + trojuhelnik hrotu
public class Arrow extends Solid {

    private static final Vec3D NZ = new Vec3D(0, 0, 1);

    public Arrow() {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        vertexBuffer.add(new Vertex(-0.5, 0.0, 0.5, new Col(0x6699ff), new Vec2D(0, 0.5), NZ));
        vertexBuffer.add(new Vertex(0.0, 0.0, 0.5, new Col(0xffaa66), new Vec2D(0.35, 0.5), NZ));
        vertexBuffer.add(new Vertex(0.2, 0.15, 0.5, new Col(0xff0000), new Vec2D(0.55, 0.85), NZ));
        vertexBuffer.add(new Vertex(0.4, 0.0, 0.5, new Col(0xff0000), new Vec2D(0.85, 0.5), NZ));
        vertexBuffer.add(new Vertex(0.2, -0.15, 0.5, new Col(0xff0000), new Vec2D(0.55, 0.15), NZ));

        addIndices(0, 1);
        addIndices(1, 2, 4);

        partBuffer.add(new Part(Topology.LINES, 1, 0));
        partBuffer.add(new Part(Topology.TRIANGLES, 1, 2));
    }

}
