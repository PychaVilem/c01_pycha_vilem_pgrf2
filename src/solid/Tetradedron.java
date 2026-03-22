package solid;

import model.Part;
import model.Topology;
import model.Vertex;
import transforms.Col;
import transforms.Mat4Identity;
import transforms.Vec2D;
import transforms.Vec3D;

import java.util.ArrayList;

// ctyrsten - pro drat 4 body, pro steny znova vrcholy s uv a normalami
public class Tetradedron extends Solid {

    public Tetradedron() {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        double a = 0.5;
        Col c0 = new Col(0xff0000);
        Col c1 = new Col(0x00ff00);
        Col c2 = new Col(0x0000ff);
        Col c3 = new Col(0xffff00);

        double x0 = 0, y0 = 0, z0 = 0;
        double x1 = a * 2, y1 = 0, z1 = 0;
        double x2 = a, y2 = a * 2, z2 = 0;
        double x3 = a, y3 = a, z3 = a * 2;

        vertexBuffer.add(new Vertex(x0, y0, z0, c0));
        vertexBuffer.add(new Vertex(x1, y1, z1, c1));
        vertexBuffer.add(new Vertex(x2, y2, z2, c2));
        vertexBuffer.add(new Vertex(x3, y3, z3, c3));

        addIndices(0, 1, 1, 2, 2, 0, 0, 3, 1, 3, 2, 3);

        addFace(x0, y0, z0, c0, x1, y1, z1, c1, x2, y2, z2, c2);
        addFace(x0, y0, z0, c0, x1, y1, z1, c1, x3, y3, z3, c3);
        addFace(x0, y0, z0, c0, x2, y2, z2, c2, x3, y3, z3, c3);
        addFace(x1, y1, z1, c1, x2, y2, z2, c2, x3, y3, z3, c3);

        partBuffer.add(new Part(Topology.LINES, 6, 0));
        partBuffer.add(new Part(Topology.TRIANGLES, 4, 12));
    }

    private void addFace(double ax, double ay, double az, Col ca,
                         double bx, double by, double bz, Col cb,
                         double cx, double cy, double cz, Col cc) {
        double ux = bx - ax, uy = by - ay, uz = bz - az;
        double vx = cx - ax, vy = cy - ay, vz = cz - az;
        Vec3D n = new Vec3D(
                uy * vz - uz * vy,
                uz * vx - ux * vz,
                ux * vy - uy * vx
        ).normalized().orElse(new Vec3D(0, 0, 1));

        int b = vertexBuffer.size();
        vertexBuffer.add(new Vertex(ax, ay, az, ca, new Vec2D(0, 0), n));
        vertexBuffer.add(new Vertex(bx, by, bz, cb, new Vec2D(1, 0), n));
        vertexBuffer.add(new Vertex(cx, cy, cz, cc, new Vec2D(0, 1), n));
        addIndices(b, b + 1, b + 2);
    }
}
