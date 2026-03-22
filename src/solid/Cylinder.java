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

// valec: plast + dve podstavy, n segmentu
public class Cylinder extends Solid {

    private static final int N = 16;

    public Cylinder() {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        double r = 0.5;
        double h = 0.5;
        Vec3D nDown = new Vec3D(0, 0, -1);
        Vec3D nUp = new Vec3D(0, 0, 1);

        vertexBuffer.add(new Vertex(0, 0, -h, new Col(0x888888), new Vec2D(0.5, 0.5), nDown));
        for (int i = 0; i < N; i++) {
            double t = 2 * Math.PI * i / N;
            double x = r * Math.cos(t);
            double y = r * Math.sin(t);
            int rgb = Color.HSBtoRGB((float) (i / (float) N), 0.72f, 0.92f);
            Col c = new Col((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
            double uc = 0.5 + 0.5 * x / r;
            double vc = 0.5 + 0.5 * y / r;
            vertexBuffer.add(new Vertex(x, y, -h, c, new Vec2D(uc, vc), nDown));
        }
        vertexBuffer.add(new Vertex(0, 0, h, new Col(0x888888), new Vec2D(0.5, 0.5), nUp));
        for (int i = 0; i < N; i++) {
            double t = 2 * Math.PI * i / N;
            double x = r * Math.cos(t);
            double y = r * Math.sin(t);
            int rgb = Color.HSBtoRGB((float) ((i / (float) N + 0.08f) % 1f), 0.5f, 1f);
            Col c = new Col((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
            double uc = 0.5 + 0.5 * x / r;
            double vc = 0.5 + 0.5 * y / r;
            vertexBuffer.add(new Vertex(x, y, h, c, new Vec2D(uc, vc), nUp));
        }

        int sideBase = vertexBuffer.size();
        for (int i = 0; i < N; i++) {
            double t = 2 * Math.PI * i / N;
            double x = r * Math.cos(t);
            double y = r * Math.sin(t);
            int rgb = Color.HSBtoRGB((float) (i / (float) N), 0.72f, 0.92f);
            Col c = new Col((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
            Vec3D rn = new Vec3D(x, y, 0).normalized().orElse(new Vec3D(1, 0, 0));
            vertexBuffer.add(new Vertex(x, y, -h, c, new Vec2D(i / (double) N, 0), rn));
        }
        for (int i = 0; i < N; i++) {
            double t = 2 * Math.PI * i / N;
            double x = r * Math.cos(t);
            double y = r * Math.sin(t);
            int rgb = Color.HSBtoRGB((float) ((i / (float) N + 0.08f) % 1f), 0.5f, 1f);
            Col c = new Col((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
            Vec3D rn = new Vec3D(x, y, 0).normalized().orElse(new Vec3D(1, 0, 0));
            vertexBuffer.add(new Vertex(x, y, h, c, new Vec2D(i / (double) N, 1), rn));
        }

        int centerBot = 0;
        int ringBot = 1;
        int centerTop = N + 1;
        int ringTop = N + 2;
        int sb = sideBase;
        int st = sideBase + N;

        for (int i = 0; i < N; i++) {
            int next = (i + 1) % N;
            addIndices(ringBot + i, ringBot + next);
            addIndices(ringTop + i, ringTop + next);
            addIndices(sb + i, st + i);
        }

        int triStart = indexBuffer.size();
        for (int i = 0; i < N; i++) {
            int next = (i + 1) % N;
            addIndices(sb + i, sb + next, st + i);
            addIndices(sb + next, st + next, st + i);
        }
        for (int i = 1; i < N; i++) {
            addIndices(centerBot, ringBot + i - 1, ringBot + i);
        }
        addIndices(centerBot, ringBot + N - 1, ringBot);
        for (int i = 1; i < N; i++) {
            addIndices(centerTop, ringTop + i - 1, ringTop + i);
        }
        addIndices(centerTop, ringTop + N - 1, ringTop);

        int lineCount = N * 3;
        int triCount = (indexBuffer.size() - triStart) / 3;
        partBuffer.add(new Part(Topology.LINES, lineCount, 0));
        partBuffer.add(new Part(Topology.TRIANGLES, triCount, triStart));
    }
}
