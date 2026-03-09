package renderer;

import model.Part;
import model.Topology;
import model.Vertex;
import rasterize.LineRasterizer;
import rasterize.TriangelRasterizer;
import solid.Axes;
import solid.Solid;
import transforms.Col;
import transforms.Mat4;
import transforms.Point3D;
import transforms.Vec3D;

public class RendererSolid {

    private final LineRasterizer lineRasterizer;
    private final TriangelRasterizer triangelRasterizer;

    private Mat4 viewMatrix;
    private Mat4 projectionMatrix;
    private int viewportWidth;
    private int viewportHeight;
    /** true = kreslit jen hrany (drátový model), false = kreslit jen plochy (trojúhelníky). */
    private boolean wireframeOnly = false;
    /** Vybrané těleso – kreslíme ho bílou barvou. Osy nemohou být vybrané. */
    private Solid activeSolid = null;

    public void setActiveSolid(Solid solid) {
        this.activeSolid = solid;
    }

    public void setWireframeOnly(boolean wireframeOnly) {
        this.wireframeOnly = wireframeOnly;
    }

    public RendererSolid(LineRasterizer lineRasterizer, TriangelRasterizer triangelRasterizer) {
        this.lineRasterizer = lineRasterizer;
        this.triangelRasterizer = triangelRasterizer;
    }

    public void setViewMatrix(Mat4 viewMatrix) {
        this.viewMatrix = viewMatrix;
    }

    public void setProjectionMatrix(Mat4 projectionMatrix) {
        this.projectionMatrix = projectionMatrix;
    }

    public void setViewportSize(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    private Vertex transformVertex(Vertex v, Mat4 mvp) {
        Point3D p = v.getPosition();
        Point3D pClip = p.mul(mvp);
        var ndcOpt = pClip.dehomog();
        if (ndcOpt.isEmpty()) {
            return null;
        }
        Vec3D ndc = ndcOpt.get();

        double ndcX = ndc.getX();
        double ndcY = ndc.getY();
        double ndcZ = ndc.getZ();

        // Ořezání podle z: NDC z je v [0, 1] (right-handed, zn->0, zf->1)
        if (ndcZ < 0 || ndcZ > 1) {
            return null;
        }

        // z NDC (-1..1) do okna (0..width-1, 0..height-1)
        double sx = (ndcX * 0.5 + 0.5) * (viewportWidth - 1);
        double sy = (1.0 - (ndcY * 0.5 + 0.5)) * (viewportHeight - 1);

        return new Vertex(sx, sy, ndcZ, v.getColor());
    }

    private static final Col COL_R = new Col(1, 0, 0);
    private static final Col COL_G = new Col(0, 1, 0);
    private static final Col COL_B = new Col(0, 0, 1);
    private static final double BLEND_AMOUNT = 0.5;

    private Col displayColor(Col base, Solid solid) {
        if (activeSolid != null && solid == activeSolid) {
            return new Col(0xffffff);
        }
        int mode = solid.getColorBlendMode();
        if (mode == 0) return base;
        Col rgb = mode == 1 ? COL_R : (mode == 2 ? COL_G : COL_B);
        return base.mul(1 - BLEND_AMOUNT).add(rgb.mul(BLEND_AMOUNT)).saturate();
    }

    public void render(Solid solid) {
        if (viewMatrix == null || projectionMatrix == null || viewportWidth == 0 || viewportHeight == 0) {
            // ještě není nastaveno, radši nic nekreslit
            return;
        }

        // pořadí pro row-vector pipeline: M * V * P
        Mat4 mvp = solid.getModelMat().mul(viewMatrix).mul(projectionMatrix);

        // cyklus co projíždí part buffer
        for (Part part : solid.getPartBuffer()) {
            // Osy XYZ vždy kreslíme celé (hrany + šipky), bez ohledu na M
            boolean skipByMode = !(solid instanceof Axes)
                    && ((wireframeOnly && part.getTopology() != Topology.LINES)
                    || (!wireframeOnly && part.getTopology() != Topology.TRIANGLES));
            if (skipByMode) continue;

            switch (part.getTopology()) {
                case LINES: {
                    int index = part.getStartIndex();
                    for (int i = 0; i < part.getCount(); i++) {
                        int indexA = solid.getIndexBuffer().get(index++);
                        int indexB = solid.getIndexBuffer().get(index++);

                        Vertex aWorld = solid.getVertexBuffer().get(indexA);
                        Vertex bWorld = solid.getVertexBuffer().get(indexB);

                        Vertex a = transformVertex(aWorld, mvp);
                        Vertex b = transformVertex(bWorld, mvp);
                        if (a == null || b == null) {
                            continue;
                        }

                        Col lineColor = displayColor(aWorld.getColor(), solid);
                        lineRasterizer.setColor(lineColor);

                        // zatím jen 2D rasterizace úsečky ve screen space
                        lineRasterizer.rasterize(
                                (int) Math.round(a.getX()),
                                (int) Math.round(a.getY()),
                                (int) Math.round(b.getX()),
                                (int) Math.round(b.getY())
                        );
                    }
                    break;
                }
                case TRIANGLES: {
                    int index = part.getStartIndex();
                    for (int i = 0; i < part.getCount(); i++) {
                        int indexA = solid.getIndexBuffer().get(index++);
                        int indexB = solid.getIndexBuffer().get(index++);
                        int indexC = solid.getIndexBuffer().get(index++);

                        Vertex aWorld = solid.getVertexBuffer().get(indexA);
                        Vertex bWorld = solid.getVertexBuffer().get(indexB);
                        Vertex cWorld = solid.getVertexBuffer().get(indexC);

                        Vertex a = transformVertex(aWorld, mvp);
                        Vertex b = transformVertex(bWorld, mvp);
                        Vertex c = transformVertex(cWorld, mvp);
                        if (a == null || b == null || c == null) {
                            continue;
                        }

                        Col ca = displayColor(aWorld.getColor(), solid);
                        Col cb = displayColor(bWorld.getColor(), solid);
                        Col cc = displayColor(cWorld.getColor(), solid);
                        Vertex aDraw = new Vertex(a.getX(), a.getY(), a.getZ(), ca);
                        Vertex bDraw = new Vertex(b.getX(), b.getY(), b.getZ(), cb);
                        Vertex cDraw = new Vertex(c.getX(), c.getY(), c.getZ(), cc);
                        triangelRasterizer.rasterize(aDraw, bDraw, cDraw);
                    }
                    break;
                }
                default:
                    // ostatní topologie teď ignorujeme
                    break;
            }
        }
    }
}
