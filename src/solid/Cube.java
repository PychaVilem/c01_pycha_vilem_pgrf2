package solid;

import model.Part;
import model.Topology;
import model.Vertex;
import transforms.Col;
import transforms.Mat4Identity;
import transforms.Vec2D;
import transforms.Vec3D;

import java.util.ArrayList;

// krychle - u ploch duplikovane vrcholy aby sla kazda stena obarvit a mit vlastni normalu
public class Cube extends Solid {

    private static final double H = 0.5;

    public Cube() {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        Col c0 = new Col(0xff0000);
        Col c1 = new Col(0x00ff00);
        Col c2 = new Col(0x0000ff);
        Col c3 = new Col(0xffff00);
        Col c4 = new Col(0xff00ff);
        Col c5 = new Col(0x00ffff);
        Col c6 = new Col(0xff8800);
        Col c7 = new Col(0xffffff);

        vertexBuffer.add(new Vertex(-H, -H, -H, c0));
        vertexBuffer.add(new Vertex(H, -H, -H, c1));
        vertexBuffer.add(new Vertex(H, H, -H, c2));
        vertexBuffer.add(new Vertex(-H, H, -H, c3));
        vertexBuffer.add(new Vertex(-H, -H, H, c4));
        vertexBuffer.add(new Vertex(H, -H, H, c5));
        vertexBuffer.add(new Vertex(H, H, H, c6));
        vertexBuffer.add(new Vertex(-H, H, H, c7));

        // Čáry v index bufferu musí být před trojúhelníky (Part: LINES od 0, TRIANGLES od 24).
        addIndices(0, 1, 1, 2, 2, 3, 3, 0);
        addIndices(4, 5, 5, 6, 6, 7, 7, 4);
        addIndices(0, 4, 1, 5, 2, 6, 3, 7);

        addFaceZneg(c0, c1, c2, c3);
        addFaceZpos(c4, c5, c6, c7);
        addFaceXpos(c1, c5, c6, c2);
        addFaceXneg(c0, c4, c7, c3);
        addFaceYpos(c3, c2, c6, c7);
        addFaceYneg(c0, c1, c5, c4);

        partBuffer.add(new Part(Topology.LINES, 12, 0));
        partBuffer.add(new Part(Topology.TRIANGLES, 12, 24));
    }

    private void addFaceZneg(Col cbl, Col cbr, Col ctr, Col ctl) {
        Vec3D n = new Vec3D(0, 0, -1);
        int b = vertexBuffer.size();
        vertexBuffer.add(new Vertex(-H, -H, -H, cbl, new Vec2D(0, 1), n));
        vertexBuffer.add(new Vertex(H, -H, -H, cbr, new Vec2D(1, 1), n));
        vertexBuffer.add(new Vertex(H, H, -H, ctr, new Vec2D(1, 0), n));
        vertexBuffer.add(new Vertex(-H, H, -H, ctl, new Vec2D(0, 0), n));
        addIndices(b, b + 1, b + 2, b, b + 2, b + 3);
    }

    private void addFaceZpos(Col cbl, Col cbr, Col ctr, Col ctl) {
        Vec3D n = new Vec3D(0, 0, 1);
        int b = vertexBuffer.size();
        vertexBuffer.add(new Vertex(-H, -H, H, cbl, new Vec2D(0, 0), n));
        vertexBuffer.add(new Vertex(H, -H, H, cbr, new Vec2D(1, 0), n));
        vertexBuffer.add(new Vertex(H, H, H, ctr, new Vec2D(1, 1), n));
        vertexBuffer.add(new Vertex(-H, H, H, ctl, new Vec2D(0, 1), n));
        addIndices(b, b + 1, b + 2, b, b + 2, b + 3);
    }

    private void addFaceXpos(Col cbl, Col cbr, Col ctr, Col ctl) {
        Vec3D n = new Vec3D(1, 0, 0);
        int b = vertexBuffer.size();
        vertexBuffer.add(new Vertex(H, -H, -H, cbl, new Vec2D(0, 1), n));
        vertexBuffer.add(new Vertex(H, -H, H, cbr, new Vec2D(1, 1), n));
        vertexBuffer.add(new Vertex(H, H, H, ctr, new Vec2D(1, 0), n));
        vertexBuffer.add(new Vertex(H, H, -H, ctl, new Vec2D(0, 0), n));
        addIndices(b, b + 1, b + 2, b, b + 2, b + 3);
    }

    private void addFaceXneg(Col cbl, Col cbr, Col ctr, Col ctl) {
        Vec3D n = new Vec3D(-1, 0, 0);
        int b = vertexBuffer.size();
        vertexBuffer.add(new Vertex(-H, -H, -H, cbl, new Vec2D(1, 1), n));
        vertexBuffer.add(new Vertex(-H, -H, H, cbr, new Vec2D(0, 1), n));
        vertexBuffer.add(new Vertex(-H, H, H, ctr, new Vec2D(0, 0), n));
        vertexBuffer.add(new Vertex(-H, H, -H, ctl, new Vec2D(1, 0), n));
        addIndices(b, b + 1, b + 2, b, b + 2, b + 3);
    }

    private void addFaceYpos(Col cbl, Col cbr, Col ctr, Col ctl) {
        Vec3D n = new Vec3D(0, 1, 0);
        int b = vertexBuffer.size();
        vertexBuffer.add(new Vertex(-H, H, -H, cbl, new Vec2D(0, 1), n));
        vertexBuffer.add(new Vertex(H, H, -H, cbr, new Vec2D(1, 1), n));
        vertexBuffer.add(new Vertex(H, H, H, ctr, new Vec2D(1, 0), n));
        vertexBuffer.add(new Vertex(-H, H, H, ctl, new Vec2D(0, 0), n));
        addIndices(b, b + 1, b + 2, b, b + 2, b + 3);
    }

    private void addFaceYneg(Col cbl, Col cbr, Col ctr, Col ctl) {
        Vec3D n = new Vec3D(0, -1, 0);
        int b = vertexBuffer.size();
        vertexBuffer.add(new Vertex(-H, -H, -H, cbl, new Vec2D(0, 0), n));
        vertexBuffer.add(new Vertex(H, -H, -H, cbr, new Vec2D(1, 0), n));
        vertexBuffer.add(new Vertex(H, -H, H, ctr, new Vec2D(1, 1), n));
        vertexBuffer.add(new Vertex(-H, -H, H, ctl, new Vec2D(0, 1), n));
        addIndices(b, b + 1, b + 2, b, b + 2, b + 3);
    }
}
