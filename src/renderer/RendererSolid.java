package renderer;

import model.Part;
import model.Topology;
import model.Vertex;
import rasterize.LineRasterizer;
import rasterize.TriangelRasterizer;
import shader.Shader;
import shader.ShaderInterpolated;
import shader.ShaderTexture;
import solid.Axes;
import solid.Solid;
import transforms.Col;
import transforms.Mat4;
import transforms.Point3D;
import transforms.Vec2D;
import transforms.Vec3D;
import util.Lerp;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class RendererSolid {

    /** Tolerance NDC x,y – objekty těsně u okraje obrazovky se neorežou (subpixel, zaokrouhlení). */
    private static final double NDC_XY_MARGIN = 0.05;
    /** Tolerance NDC z – mírné povolení u near/far, aby se neorezávalo příliš brzy. */
    private static final double NDC_Z_TOLERANCE = 0.001;
    /** V clip space: bod je „před kamerou“, pokud z >= -eps*w (numerická tolerance). */
    private static final double CLIP_Z_EPSILON = 1e-5;

    /** Vrchol v clip space (homogenní souřadnice) s atributy pro interpolaci při ořezu. */
    private static class ClipVertex {
        final Point3D p;
        final Col color;
        final Vec2D uv;

        ClipVertex(Point3D p, Col color, Vec2D uv) {
            this.p = p;
            this.color = color != null ? color : new Col(0xffffff);
            this.uv = uv;
        }
    }

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
    /** Globální textura – může ji používat ShaderTexture. */
    private BufferedImage texture;

    public void setActiveSolid(Solid solid) {
        this.activeSolid = solid;
    }

    public void setWireframeOnly(boolean wireframeOnly) {
        this.wireframeOnly = wireframeOnly;
    }

    public void setTexture(BufferedImage texture) {
        this.texture = texture;
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
        triangelRasterizer.setViewport(width, height);
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

        // Ořezání podle z: NDC z v [0, 1] s tolerancí, aby se neorezávalo příliš brzy
        if (ndcZ < -NDC_Z_TOLERANCE || ndcZ > 1.0 + NDC_Z_TOLERANCE) {
            return null;
        }

        // Podmínkové ořezávání (varianta A): NDC x, y s mírnou tolerancí (subpixel, zaokrouhlení)
        if (ndcX < -1.0 - NDC_XY_MARGIN || ndcX > 1.0 + NDC_XY_MARGIN
                || ndcY < -1.0 - NDC_XY_MARGIN || ndcY > 1.0 + NDC_XY_MARGIN) {
            return null;
        }

        // z NDC (-1..1) do okna (0..width-1, 0..height-1)
        double sx = (ndcX * 0.5 + 0.5) * (viewportWidth - 1);
        double sy = (1.0 - (ndcY * 0.5 + 0.5)) * (viewportHeight - 1);

        // zachováme i uv souřadnice pro texturování
        return new Vertex(sx, sy, ndcZ, v.getColor(), v.getUv());
    }

    /** Převod vrcholu z clip space na screen-space vertex. Null jen při w=0 (dehomog selhal). */
    private Vertex clipSpaceToVertex(ClipVertex cv) {
        var ndcOpt = cv.p.dehomog();
        if (ndcOpt.isEmpty()) return null;
        Vec3D ndc = ndcOpt.get();
        double ndcX = ndc.getX(), ndcY = ndc.getY(), ndcZ = ndc.getZ();
        double sx = (ndcX * 0.5 + 0.5) * (viewportWidth - 1);
        double sy = (1.0 - (ndcY * 0.5 + 0.5)) * (viewportHeight - 1);
        return new Vertex(sx, sy, ndcZ, cv.color, cv.uv);
    }

    /** Ořez trojúhelníku (ve screen space) na viewport [0,w]×[0,h]; vrací 0 až několik trojúhelníků. */
    private List<List<Vertex>> clipTriangleToViewport(Vertex a, Vertex b, Vertex c) {
        List<Vertex> poly = new ArrayList<>();
        poly.add(a);
        poly.add(b);
        poly.add(c);
        double xMax = viewportWidth - 1;
        double yMax = viewportHeight - 1;
        // Sutherland–Hodgman: 4 hranice viewportu
        poly = clipPolygonToHalfplane(poly, v -> v.getX() >= 0, (p, q) -> intersectX(p, q, 0));
        if (poly.size() < 3) return List.of();
        poly = clipPolygonToHalfplane(poly, v -> v.getX() <= xMax, (p, q) -> intersectX(p, q, xMax));
        if (poly.size() < 3) return List.of();
        poly = clipPolygonToHalfplane(poly, v -> v.getY() >= 0, (p, q) -> intersectY(p, q, 0));
        if (poly.size() < 3) return List.of();
        poly = clipPolygonToHalfplane(poly, v -> v.getY() <= yMax, (p, q) -> intersectY(p, q, yMax));
        if (poly.size() < 3) return List.of();
        return triangulateFanVertices(poly);
    }

    private static Vertex lerpVertex(Vertex a, Vertex b, double t) {
        double x = Lerp.lerp(a.getX(), b.getX(), t);
        double y = Lerp.lerp(a.getY(), b.getY(), t);
        double z = Lerp.lerp(a.getZ(), b.getZ(), t);
        Col c = Lerp.lerp(a.getColor(), b.getColor(), t);
        Vec2D uv = (a.getUv() != null && b.getUv() != null)
                ? new Vec2D(Lerp.lerp(a.getUv().getX(), b.getUv().getX(), t),
                        Lerp.lerp(a.getUv().getY(), b.getUv().getY(), t))
                : null;
        return new Vertex(x, y, z, c, uv);
    }

    private static Vertex intersectX(Vertex a, Vertex b, double xEdge) {
        double denom = b.getX() - a.getX();
        double t = (Math.abs(denom) < 1e-10) ? 0.5 : (xEdge - a.getX()) / denom;
        t = Math.max(0, Math.min(1, t));
        return lerpVertex(a, b, t);
    }

    private static Vertex intersectY(Vertex a, Vertex b, double yEdge) {
        double denom = b.getY() - a.getY();
        double t = (Math.abs(denom) < 1e-10) ? 0.5 : (yEdge - a.getY()) / denom;
        t = Math.max(0, Math.min(1, t));
        return lerpVertex(a, b, t);
    }

    @FunctionalInterface
    private interface InsideTest2D { boolean inside(Vertex v); }
    @FunctionalInterface
    private interface Intersect2D { Vertex intersect(Vertex a, Vertex b); }

    private static List<Vertex> clipPolygonToHalfplane(List<Vertex> polygon, InsideTest2D inside, Intersect2D intersect) {
        if (polygon.size() < 3) return polygon;
        List<Vertex> out = new ArrayList<>();
        for (int i = 0; i < polygon.size(); i++) {
            Vertex curr = polygon.get(i);
            Vertex next = polygon.get((i + 1) % polygon.size());
            boolean currIn = inside.inside(curr);
            boolean nextIn = inside.inside(next);
            if (currIn && nextIn) {
                out.add(next);
            } else if (currIn && !nextIn) {
                out.add(intersect.intersect(curr, next));
            } else if (!currIn && nextIn) {
                out.add(intersect.intersect(curr, next));
                out.add(next);
            }
        }
        return out;
    }

    private static List<List<Vertex>> triangulateFanVertices(List<Vertex> polygon) {
        List<List<Vertex>> triangles = new ArrayList<>();
        if (polygon.size() < 3) return triangles;
        for (int i = 1; i + 1 < polygon.size(); i++) {
            triangles.add(List.of(polygon.get(0), polygon.get(i), polygon.get(i + 1)));
        }
        return triangles;
    }

    /** Ořezání polygonu rovinou z = 0; zachovává body s z >= -eps*w (numerická tolerance). */
    private static List<ClipVertex> clipPolygonByZ0(List<ClipVertex> polygon) {
        return clipPolygonByPlane(polygon,
                cv -> cv.p.getZ() >= -CLIP_Z_EPSILON * Math.abs(cv.p.getW()),
                (a, b) -> {
                    double t = a.p.getZ() == b.p.getZ() ? 0.5 : -a.p.getZ() / (b.p.getZ() - a.p.getZ());
                    t = Math.max(0, Math.min(1, t));
                    Point3D p = lerp(a.p, b.p, t);
                    Col c = Lerp.lerp(a.color, b.color, t);
                    Vec2D uv = (a.uv != null && b.uv != null)
                            ? new Vec2D(Lerp.lerp(a.uv.getX(), b.uv.getX(), t), Lerp.lerp(a.uv.getY(), b.uv.getY(), t))
                            : null;
                    return new ClipVertex(p, c, uv);
                });
    }

    /** Ořezání polygonu rovinou w = z; zachovává body s w >= z (s malou tolerancí). */
    private static List<ClipVertex> clipPolygonByWEqualsZ(List<ClipVertex> polygon) {
        return clipPolygonByPlane(polygon,
                cv -> cv.p.getW() >= cv.p.getZ() - CLIP_Z_EPSILON * Math.abs(cv.p.getW()),
                (a, b) -> {
                    double denom = (b.p.getW() - b.p.getZ()) - (a.p.getW() - a.p.getZ());
                    double t = (denom == 0) ? 0.5 : (a.p.getZ() - a.p.getW()) / denom;
                    t = Math.max(0, Math.min(1, t));
                    Point3D p = lerp(a.p, b.p, t);
                    Col c = Lerp.lerp(a.color, b.color, t);
                    Vec2D uv = (a.uv != null && b.uv != null)
                            ? new Vec2D(Lerp.lerp(a.uv.getX(), b.uv.getX(), t), Lerp.lerp(a.uv.getY(), b.uv.getY(), t))
                            : null;
                    return new ClipVertex(p, c, uv);
                });
    }

    private static Point3D lerp(Point3D a, Point3D b, double t) {
        return new Point3D(
                Lerp.lerp(a.getX(), b.getX(), t),
                Lerp.lerp(a.getY(), b.getY(), t),
                Lerp.lerp(a.getZ(), b.getZ(), t),
                Lerp.lerp(a.getW(), b.getW(), t));
    }

    @FunctionalInterface
    private interface InsideTest { boolean inside(ClipVertex v); }
    @FunctionalInterface
    private interface IntersectFunc { ClipVertex intersect(ClipVertex a, ClipVertex b); }

    private static List<ClipVertex> clipPolygonByPlane(List<ClipVertex> polygon, InsideTest inside, IntersectFunc intersect) {
        if (polygon.size() < 3) return polygon;
        List<ClipVertex> out = new ArrayList<>();
        for (int i = 0; i < polygon.size(); i++) {
            ClipVertex curr = polygon.get(i);
            ClipVertex next = polygon.get((i + 1) % polygon.size());
            boolean currIn = inside.inside(curr);
            boolean nextIn = inside.inside(next);
            if (currIn && nextIn) {
                out.add(next);
            } else if (currIn && !nextIn) {
                out.add(intersect.intersect(curr, next));
            } else if (!currIn && nextIn) {
                out.add(intersect.intersect(curr, next));
                out.add(next);
            }
        }
        return out;
    }

    /** Rozloží konvexní polygon na trojúhelníky (fan od prvního vrcholu). */
    private static List<List<ClipVertex>> triangulateFan(List<ClipVertex> polygon) {
        List<List<ClipVertex>> triangles = new ArrayList<>();
        if (polygon.size() < 3) return triangles;
        for (int i = 1; i + 1 < polygon.size(); i++) {
            List<ClipVertex> tri = new ArrayList<>();
            tri.add(polygon.get(0));
            tri.add(polygon.get(i));
            tri.add(polygon.get(i + 1));
            triangles.add(tri);
        }
        return triangles;
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
                    // Vybereme shader podle nastavení na solidu
                    Shader shader = solid.getShader();
                    if (shader instanceof ShaderTexture && texture == null) {
                        // když není textura, spadneme na interpolovaný shader
                        shader = new ShaderInterpolated();
                        solid.setShader(shader);
                    }
                    if (shader instanceof ShaderTexture && texture != null) {
                        // zajistíme, že ShaderTexture má aktuální texturu
                        shader = new ShaderTexture(texture);
                        solid.setShader(shader);
                    }
                    triangelRasterizer.setShader(shader);
                    for (int i = 0; i < part.getCount(); i++) {
                        int indexA = solid.getIndexBuffer().get(index++);
                        int indexB = solid.getIndexBuffer().get(index++);
                        int indexC = solid.getIndexBuffer().get(index++);

                        Vertex aWorld = solid.getVertexBuffer().get(indexA);
                        Vertex bWorld = solid.getVertexBuffer().get(indexB);
                        Vertex cWorld = solid.getVertexBuffer().get(indexC);

                        // Ořezání podle Z před dehomogenizací: clip space (z >= 0, w >= z)
                        Point3D aClip = aWorld.getPosition().mul(mvp);
                        Point3D bClip = bWorld.getPosition().mul(mvp);
                        Point3D cClip = cWorld.getPosition().mul(mvp);
                        List<ClipVertex> poly = new ArrayList<>();
                        poly.add(new ClipVertex(aClip, aWorld.getColor(), aWorld.getUv()));
                        poly.add(new ClipVertex(bClip, bWorld.getColor(), bWorld.getUv()));
                        poly.add(new ClipVertex(cClip, cWorld.getColor(), cWorld.getUv()));
                        poly = clipPolygonByZ0(poly);
                        if (poly.size() < 3) continue;
                        poly = clipPolygonByWEqualsZ(poly);
                        if (poly.size() < 3) continue;
                        List<List<ClipVertex>> clippedTriangles = triangulateFan(poly);
                        for (List<ClipVertex> tri : clippedTriangles) {
                            Vertex a = clipSpaceToVertex(tri.get(0));
                            Vertex b = clipSpaceToVertex(tri.get(1));
                            Vertex c = clipSpaceToVertex(tri.get(2));
                            if (a == null || b == null || c == null) continue;
                            // Ořez na viewport – vykreslíme jen viditelnou část, ne celou plochu
                            List<List<Vertex>> viewportTriangles = clipTriangleToViewport(a, b, c);
                            for (List<Vertex> vt : viewportTriangles) {
                                Vertex va = vt.get(0), vb = vt.get(1), vc = vt.get(2);
                                Col ca = displayColor(va.getColor(), solid);
                                Col cb = displayColor(vb.getColor(), solid);
                                Col cc = displayColor(vc.getColor(), solid);
                                Vertex aDraw = new Vertex(new Point3D(va.getX(), va.getY(), va.getZ()), ca, va.getUv());
                                Vertex bDraw = new Vertex(new Point3D(vb.getX(), vb.getY(), vb.getZ()), cb, vb.getUv());
                                Vertex cDraw = new Vertex(new Point3D(vc.getX(), vc.getY(), vc.getZ()), cc, vc.getUv());
                                triangelRasterizer.rasterize(aDraw, bDraw, cDraw);
                            }
                        }
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