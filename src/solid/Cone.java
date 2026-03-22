package solid;

import model.Part;
import model.Topology;
import model.Vertex;
import transforms.Col;
import transforms.Mat4Identity;
import transforms.Vec2D;
import transforms.Vec3D;

import java.awt.*;
import java.util.ArrayList;

// kuzel: plast po obvodu + kruhova podstava, uv pro texturu
public class Cone extends Solid {

    private static final int N = 16;

    public Cone() {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        double r = 0.5;
        double h = 0.5;
        Vec3D nDown = new Vec3D(0, 0, -1);

        vertexBuffer.add(new Vertex(0, 0, h, new Col(0xffe8b0), new Vec2D(0.5, 0), new Vec3D(0, 0, 1)));
        for (int i = 0; i < N; i++) {
            double t = 2 * Math.PI * i / N;
            double x = r * Math.cos(t);
            double y = r * Math.sin(t);
            int rgb = Color.HSBtoRGB((float) (i / (float) N), 0.75f, 0.88f);
            Col c = new Col((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
            Vec3D rn = new Vec3D(x, y, -h).normalized().orElse(new Vec3D(0, 0, -1));
            vertexBuffer.add(new Vertex(x, y, -h, c, new Vec2D(i / (double) N, 1), rn));
        }

        int diskCenter = vertexBuffer.size();
        vertexBuffer.add(new Vertex(0, 0, -h, new Col(0xaaaaaa), new Vec2D(0.5, 0.5), nDown));
        for (int i = 0; i < N; i++) {
            double t = 2 * Math.PI * i / N;
            double x = r * Math.cos(t);
            double y = r * Math.sin(t);
            int rgb = Color.HSBtoRGB((float) (i / (float) N), 0.75f, 0.88f);
            Col c = new Col((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
            double u = 0.5 + 0.5 * x / r;
            double v = 0.5 + 0.5 * y / r;
            vertexBuffer.add(new Vertex(x, y, -h, c, new Vec2D(u, v), nDown));
        }

        int apex = 0;
        int baseSide = 1;
        int dCenter = diskCenter;
        int dRing = diskCenter + 1;

        for (int i = 0; i < N; i++) {
            int next = (i + 1) % N;
            addIndices(apex, baseSide + i);
            addIndices(baseSide + i, baseSide + next);
        }

        int triStart = indexBuffer.size();
        for (int i = 0; i < N; i++) {
            int next = (i + 1) % N;
            addIndices(apex, baseSide + i, baseSide + next);
        }
        for (int i = 2; i < N; i++) {
            addIndices(dCenter, dRing + i - 1, dRing + i);
        }

        int lineCount = N * 2;
        int triCount = (indexBuffer.size() - triStart) / 3;
        partBuffer.add(new Part(Topology.LINES, lineCount, 0));
        partBuffer.add(new Part(Topology.TRIANGLES, triCount, triStart));
    }
}
