package renderer;

import model.Part;
import model.Vertex;
import rasterize.LineRasterizer;
import rasterize.TriangelRasterizer;
import solid.Solid;
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

        // z NDC (-1..1) do okna (0..width-1, 0..height-1)
        double sx = (ndcX * 0.5 + 0.5) * (viewportWidth - 1);
        double sy = (1.0 - (ndcY * 0.5 + 0.5)) * (viewportHeight - 1);

        return new Vertex(sx, sy, ndcZ, v.getColor());
    }

    public void render(Solid solid) {
        if (viewMatrix == null || projectionMatrix == null || viewportWidth == 0 || viewportHeight == 0) {
            // ještě není nastaveno, radši nic nekreslit
            return;
        }

        Mat4 mvp = projectionMatrix.mul(viewMatrix).mul(solid.getModelMat());

        // cyklus co projíždí part buffer
        for (Part part : solid.getPartBuffer()) {
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

                        // zatím jen předáme vrcholy přímo rasterizéru
                        triangelRasterizer.rasterize(a, b, c);
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
