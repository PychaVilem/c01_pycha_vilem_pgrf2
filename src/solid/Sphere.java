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

// koule jako sit latitud + longitud, stred v 0 polomer v parametru
public class Sphere extends Solid {

    private static final int DEFAULT_STACKS = 16;
    private static final int DEFAULT_SLICES = 24;
    private static final double DEFAULT_RADIUS = 0.5;

    public Sphere() {
        this(DEFAULT_RADIUS, DEFAULT_STACKS, DEFAULT_SLICES, new Col(0xdde4ff));
    }

    public Sphere(double radius, int stacks, int slices, Col color) {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        int safeStacks = Math.max(3, stacks);
        int safeSlices = Math.max(3, slices);

        // mrizka vrcholu pro uv (sev se zavre tim ze posledni slice = prvni)
        for (int stack = 0; stack <= safeStacks; stack++) {
            double v = stack / (double) safeStacks;
            double phi = Math.PI * v; // 0..PI
            double sinPhi = Math.sin(phi);
            double cosPhi = Math.cos(phi);

            for (int slice = 0; slice <= safeSlices; slice++) {
                double u = slice / (double) safeSlices;
                double theta = 2.0 * Math.PI * u; // 0..2PI

                double x = radius * Math.cos(theta) * sinPhi;
                double y = radius * Math.sin(theta) * sinPhi;
                double z = radius * cosPhi;

                Col vc;
                if (radius < 0.22 && color != null) {
                    vc = new Col(color);
                } else {
                    float hu = (float) (u * 0.9 + v * 0.1);
                    float sat = 0.35f + 0.45f * (float) v;
                    float bri = 0.72f + 0.25f * (float) (1.0 - v);
                    int rgb = Color.HSBtoRGB(hu % 1f, sat, bri);
                    vc = new Col((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
                }

                Vec3D normal = new Vec3D(x, y, z).normalized().orElse(new Vec3D(0, 0, 1));
                vertexBuffer.add(new Vertex(x, y, z, vc, new Vec2D(u, 1.0 - v), normal));
            }
        }

        int row = safeSlices + 1;

        int lineStart = indexBuffer.size();
        for (int stack = 0; stack <= safeStacks; stack++) {
            for (int slice = 0; slice < safeSlices; slice++) {
                int i0 = stack * row + slice;
                int i1 = i0 + 1;
                addIndices(i0, i1);
            }
        }
        for (int stack = 0; stack < safeStacks; stack++) {
            for (int slice = 0; slice <= safeSlices; slice++) {
                int i0 = stack * row + slice;
                int i1 = (stack + 1) * row + slice;
                addIndices(i0, i1);
            }
        }
        int lineCount = (indexBuffer.size() - lineStart) / 2;
        partBuffer.add(new Part(Topology.LINES, lineCount, lineStart));

        int triStart = indexBuffer.size();
        for (int stack = 0; stack < safeStacks; stack++) {
            for (int slice = 0; slice < safeSlices; slice++) {
                int a = stack * row + slice;
                int b = a + 1;
                int c = (stack + 1) * row + slice;
                int d = c + 1;
                addIndices(a, c, b);
                addIndices(b, c, d);
            }
        }
        int triCount = (indexBuffer.size() - triStart) / 3;
        partBuffer.add(new Part(Topology.TRIANGLES, triCount, triStart));
    }
}
