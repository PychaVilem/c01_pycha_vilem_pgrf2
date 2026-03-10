package solid;

import model.Part;
import model.Topology;
import model.Vertex;
import transforms.Col;
import transforms.Mat4Identity;

import java.util.ArrayList;

/**
 * Kužel (cone) podél osy Z: vrchol v (0,0,h), základna kruh v z=-h, poloměr r.
 * N bodů na základně + 1 vrchol = N+1 vrcholů.
 */
public class Cone extends Solid {

    private static final int N = 16;

    public Cone() {
        super(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Mat4Identity());

        Col color = new Col(0xaa44cc);
        double r = 0.5;
        double h = 0.5;

        vertexBuffer.add(new Vertex(0, 0, h, color)); // 0 = vrchol
        for (int i = 0; i < N; i++) {
            double t = 2 * Math.PI * i / N;
            vertexBuffer.add(new Vertex(r * Math.cos(t), r * Math.sin(t), -h, color));
        }
        int apex = 0;
        int base = 1;

        for (int i = 0; i < N; i++) {
            int next = (i + 1) % N;
            addIndices(apex, base + i);
            addIndices(base + i, base + next);
        }
        for (int i = 0; i < N; i++) {
            int next = (i + 1) % N;
            addIndices(apex, base + i, base + next);
        }
        for (int i = 2; i < N; i++) {
            addIndices(base + 0, base + i - 1, base + i);
        }

        int lineCount = N * 2;
        int triCount = N + (N - 2);
        partBuffer.add(new Part(Topology.LINES, lineCount, 0));
        partBuffer.add(new Part(Topology.TRIANGLES, triCount, lineCount * 2));
    }
}

