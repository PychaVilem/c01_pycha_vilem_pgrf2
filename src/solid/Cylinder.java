package solid;

import model.Part;
import model.Topology;
import model.Vertex;
import transforms.Col;
import transforms.Mat4Identity;

import java.util.ArrayList;

/**
 * Válec podél osy Z, střed v počátku, poloměr 0.5, výška 1 (z od -0.5 do 0.5).
 * Dvě kružnice po N bodech (N=16), plášť + dvě podstavy.
 */
public class Cylinder extends Solid {

    private static final int N = 16;

    public Cylinder() {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        Col color = new Col(0xcc6600);
        double r = 0.5;
        double h = 0.5;

        for (int i = 0; i < N; i++) {
            double t = 2 * Math.PI * i / N;
            double x = r * Math.cos(t);
            double y = r * Math.sin(t);
            vertexBuffer.add(new Vertex(x, y, -h, color));
        }
        for (int i = 0; i < N; i++) {
            double t = 2 * Math.PI * i / N;
            double x = r * Math.cos(t);
            double y = r * Math.sin(t);
            vertexBuffer.add(new Vertex(x, y, h, color));
        }
        int baseBottom = 0;
        int baseTop = N;

        for (int i = 0; i < N; i++) {
            int next = (i + 1) % N;
            addIndices(baseBottom + i, baseBottom + next);
            addIndices(baseTop + i, baseTop + next);
            addIndices(baseBottom + i, baseTop + i);
        }
        for (int i = 0; i < N; i++) {
            int next = (i + 1) % N;
            addIndices(baseBottom + i, baseBottom + next, baseTop + i);
            addIndices(baseBottom + next, baseTop + next, baseTop + i);
        }
        for (int i = 2; i < N; i++) {
            addIndices(baseBottom + 0, baseBottom + i - 1, baseBottom + i);
        }
        for (int i = 2; i < N; i++) {
            addIndices(baseTop + 0, baseTop + i, baseTop + i - 1);
        }

        int lineCount = N * 3;
        int triCount = N * 2 + (N - 2) * 2;
        int lineIndices = lineCount * 2;
        partBuffer.add(new Part(Topology.LINES, lineCount, 0));
        partBuffer.add(new Part(Topology.TRIANGLES, triCount, lineIndices));
    }
}

