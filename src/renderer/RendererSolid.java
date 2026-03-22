package renderer;

import model.Part;
import model.Topology;
import model.Vertex;
import rasterize.LineRasterizer;
import rasterize.TriangelRasterizer;
import shader.Shader;
import shader.ShaderInterpolated;
import shader.ShaderLit;
import shader.ShaderTexture;
import solid.Axes;
import solid.Solid;
import transforms.Col;
import transforms.Mat4;
import transforms.Point3D;
import transforms.Vec2D;
import transforms.Vec3D;
import util.Lerp;

import java.util.ArrayList;
import java.util.List;

// RendererSolid: vezme jedno teleso (Solid) a vykresli ho do rasteru.
//
// Rad veci u trojuhelniku:
//  1) world: enrichWorldVertex = pozice zustane modelova, pridam world pozici a otocenou normalu (kvuli phongu)
//  2) clip space: pozice * mvp, trojuhelnik orezuju rovinami z>=0 a w>=z (cast frustra)
//  3) po orezu rozdelim konvexni polygon na trojuhelniky (fan), dehomogenizace -> NDC -> pixely
//  4) jeste orez v obrazovce na obdelnik okna (clipTriangleToViewport)
//  5) TriangelRasterizer vyplni vnitrek, shader pocita barvu, ZBuffer rozhodne viditelnost
//
// U usecek jen transformVertex (rychly orez v NDC) a LineRasterizer - bez z testu na hranach.
public class RendererSolid {

    // trochu toleranc u okraje ndc at se neorezava uplne vsechno kvuli zaokrouhleni
    private static final double NDC_XY_MARGIN = 0.05;
    private static final double NDC_Z_TOLERANCE = 0.001;
    private static final double CLIP_Z_EPSILON = 1e-5;

    // vrchol jeste v clip space (homogenni) + co se ma interpolovat pri orezu
    private static class ClipVertex {
        final Point3D p;
        final Col color;
        final Vec2D uv;
        final Vec3D normal;
        final Point3D worldPosition;

        ClipVertex(Point3D p, Col color, Vec2D uv, Vec3D normal, Point3D worldPosition) {
            this.p = p;
            this.color = color != null ? color : new Col(0xffffff);
            this.uv = uv;
            this.normal = normal;
            this.worldPosition = worldPosition;
        }
    }

    private final LineRasterizer lineRasterizer;
    private final TriangelRasterizer triangelRasterizer;

    private Mat4 viewMatrix;
    private Mat4 projectionMatrix;
    private int viewportWidth;
    private int viewportHeight;
    // m = jen drat, jinak plne trojuhelniky
    private boolean wireframeOnly = false;
    // aktivni objekt lehce zvyraznim barvou
    private Solid activeSolid = null;
    private Point3D lightPositionWorld = new Point3D(0, 0, 0.5);
    private Col lightColorWorld = new Col(1.0, 1.0, 1.0);
    private Vec3D cameraPositionWorld = new Vec3D(0, 0, 3);
    // kdyz false, ShaderLit nepocita phong jen vrati zakladni barvu (nahled materialu)
    private boolean phongLightingEnabled = true;

    public void setActiveSolid(Solid solid) {
        this.activeSolid = solid;
    }

    public void setWireframeOnly(boolean wireframeOnly) {
        this.wireframeOnly = wireframeOnly;
    }

    public void setLightPositionWorld(Point3D lightPositionWorld) {
        if (lightPositionWorld != null) {
            this.lightPositionWorld = lightPositionWorld;
        }
    }

    public void setLightColorWorld(Col lightColorWorld) {
        if (lightColorWorld != null) {
            this.lightColorWorld = lightColorWorld;
        }
    }

    public void setCameraPositionWorld(Vec3D cameraPositionWorld) {
        if (cameraPositionWorld != null) {
            this.cameraPositionWorld = cameraPositionWorld;
        }
    }

    public void setPhongLightingEnabled(boolean phongLightingEnabled) {
        this.phongLightingEnabled = phongLightingEnabled;
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

    // bod z modelu pres mvp -> obrazovka, kdyz je mimo frustum vracim null (orez pred rasterizaci)
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

        // hloubka v ndc musi byt mezi near a far (tady uz po projekci jako z v 0..1)
        if (ndcZ < -NDC_Z_TOLERANCE || ndcZ > 1.0 + NDC_Z_TOLERANCE) {
            return null;
        }

        // kdyz je bod mimo ctverec -1..1 v xy, usecku vubec nekreslim (zjednoduseny orez)
        if (ndcX < -1.0 - NDC_XY_MARGIN || ndcX > 1.0 + NDC_XY_MARGIN
                || ndcY < -1.0 - NDC_XY_MARGIN || ndcY > 1.0 + NDC_XY_MARGIN) {
            return null;
        }

        double sx = (ndcX * 0.5 + 0.5) * (viewportWidth - 1);
        // y osa obrazovky je dolu, ndc y nahoru -> proto prevrat
        double sy = (1.0 - (ndcY * 0.5 + 0.5)) * (viewportHeight - 1);

        return new Vertex(new Point3D(sx, sy, ndcZ), v.getColor(), v.getUv(), v.getNormal(), v.getWorldPosition());
    }

    // po orezu v clip space: stejny prevod ndc -> pixel jako u transformVertex
    private Vertex clipSpaceToVertex(ClipVertex cv) {
        var ndcOpt = cv.p.dehomog();
        if (ndcOpt.isEmpty()) return null;
        Vec3D ndc = ndcOpt.get();
        double ndcX = ndc.getX(), ndcY = ndc.getY(), ndcZ = ndc.getZ();
        double sx = (ndcX * 0.5 + 0.5) * (viewportWidth - 1);
        double sy = (1.0 - (ndcY * 0.5 + 0.5)) * (viewportHeight - 1);
        return new Vertex(new Point3D(sx, sy, ndcZ), cv.color, cv.uv, cv.normal, cv.worldPosition);
    }

    // trojuhelnik uz je v pixelech; oreznu na [0,w-1]x[0,h-1] - mohl vzniknout vic nez jeden trojuhelnik
    private List<List<Vertex>> clipTriangleToViewport(Vertex a, Vertex b, Vertex c) {
        List<Vertex> poly = new ArrayList<>();
        poly.add(a);
        poly.add(b);
        poly.add(c);
        double xMax = viewportWidth - 1;
        double yMax = viewportHeight - 1;
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
        Vec3D normal = (a.getNormal() != null && b.getNormal() != null)
                ? new Vec3D(
                Lerp.lerp(a.getNormal().getX(), b.getNormal().getX(), t),
                Lerp.lerp(a.getNormal().getY(), b.getNormal().getY(), t),
                Lerp.lerp(a.getNormal().getZ(), b.getNormal().getZ(), t)
        ).normalized().orElse(null)
                : null;
        Point3D worldPosition = (a.getWorldPosition() != null && b.getWorldPosition() != null)
                ? new Point3D(
                Lerp.lerp(a.getWorldPosition().getX(), b.getWorldPosition().getX(), t),
                Lerp.lerp(a.getWorldPosition().getY(), b.getWorldPosition().getY(), t),
                Lerp.lerp(a.getWorldPosition().getZ(), b.getWorldPosition().getZ(), t)
        )
                : null;
        return new Vertex(new Point3D(x, y, z), c, uv, normal, worldPosition);
    }

    // pri strihu hrany polygonu o rovinu x=const nebo y=const - interpolace vsech atributu
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
                    Vec3D n = (a.normal != null && b.normal != null)
                            ? new Vec3D(
                            Lerp.lerp(a.normal.getX(), b.normal.getX(), t),
                            Lerp.lerp(a.normal.getY(), b.normal.getY(), t),
                            Lerp.lerp(a.normal.getZ(), b.normal.getZ(), t)
                    ).normalized().orElse(null)
                            : null;
                    Point3D wp = (a.worldPosition != null && b.worldPosition != null)
                            ? new Point3D(
                            Lerp.lerp(a.worldPosition.getX(), b.worldPosition.getX(), t),
                            Lerp.lerp(a.worldPosition.getY(), b.worldPosition.getY(), t),
                            Lerp.lerp(a.worldPosition.getZ(), b.worldPosition.getZ(), t)
                    )
                            : null;
                    return new ClipVertex(p, c, uv, n, wp);
                });
    }

    // dalsi cast frustra v clip space
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
                    Vec3D n = (a.normal != null && b.normal != null)
                            ? new Vec3D(
                            Lerp.lerp(a.normal.getX(), b.normal.getX(), t),
                            Lerp.lerp(a.normal.getY(), b.normal.getY(), t),
                            Lerp.lerp(a.normal.getZ(), b.normal.getZ(), t)
                    ).normalized().orElse(null)
                            : null;
                    Point3D wp = (a.worldPosition != null && b.worldPosition != null)
                            ? new Point3D(
                            Lerp.lerp(a.worldPosition.getX(), b.worldPosition.getX(), t),
                            Lerp.lerp(a.worldPosition.getY(), b.worldPosition.getY(), t),
                            Lerp.lerp(a.worldPosition.getZ(), b.worldPosition.getZ(), t)
                    )
                            : null;
                    return new ClipVertex(p, c, uv, n, wp);
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
    // lehce zluty nimbus na aktivnim objektu
    private static final Col SELECTION_TINT = new Col(1.0, 1.0, 0.88);
    private static final double SELECTION_BLEND = 0.2;

    private Col displayColor(Col base, Solid solid) {
        if (activeSolid != null && solid == activeSolid) {
            return base.mul(1.0 - SELECTION_BLEND).add(SELECTION_TINT.mul(SELECTION_BLEND)).saturate();
        }
        int mode = solid.getColorBlendMode();
        if (mode == 0) return base;
        Col rgb = mode == 1 ? COL_R : (mode == 2 ? COL_G : COL_B);
        return base.mul(1 - BLEND_AMOUNT).add(rgb.mul(BLEND_AMOUNT)).saturate();
    }

    // doplnim world pozici a normalu (phong pocita ve svetovych souradnicich)
    private static Vertex enrichWorldVertex(Vertex local, Mat4 modelMat) {
        Point3D worldPos = local.getPosition().mul(modelMat);
        Vec3D localNormal = local.getNormal();
        if (localNormal == null) {
            localNormal = new Vec3D(local.getPosition()).normalized().orElse(new Vec3D(0, 0, 1));
        }
        Vec3D worldNormal = null;
        if (localNormal != null) {
            Point3D n4 = new Point3D(localNormal.getX(), localNormal.getY(), localNormal.getZ(), 0);
            worldNormal = n4.mul(modelMat).ignoreW().normalized().orElse(null);
        }
        return new Vertex(local.getPosition(), local.getColor(), local.getUv(), worldNormal, worldPos);
    }

    public void render(Solid solid) {
        if (viewMatrix == null || projectionMatrix == null || viewportWidth == 0 || viewportHeight == 0) {
            return;
        }

        // mvp: bod ve vypoctu bod.mul(mvp) - konvence z prednasek (radkovy vektor)
        Mat4 model = solid.getModelMat();
        Mat4 mvp = model.mul(viewMatrix).mul(projectionMatrix);

        // kazdy part = kus usecek nebo trojuhelniku
        for (Part part : solid.getPartBuffer()) {
            // osy chci videt v dratu i ve vyplni zaroven
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
                    // ploche: shader nastavim jednou za part (obsahuje phong + pripadne texturu uvnitr ShaderLit)
                    // trojuhelnik: orez v clip space -> male trojuhelniky -> orez na viewport -> rasterizace + z test
                    int index = part.getStartIndex();
                    Shader shader = solid.getShader();
                    if (shader instanceof ShaderTexture st && st.getTexture() == null) {
                        shader = new ShaderLit(new ShaderInterpolated());
                    }
                    if (shader instanceof ShaderLit lit) {
                        lit.setLightingEnabled(phongLightingEnabled);
                        lit.setLightPositionWorld(lightPositionWorld);
                        lit.setLightColor(lightColorWorld);
                        lit.setCameraPositionWorld(cameraPositionWorld);
                    }
                    triangelRasterizer.setShader(shader);
                    for (int i = 0; i < part.getCount(); i++) {
                        int indexA = solid.getIndexBuffer().get(index++);
                        int indexB = solid.getIndexBuffer().get(index++);
                        int indexC = solid.getIndexBuffer().get(index++);

                        Vertex aWorld = enrichWorldVertex(solid.getVertexBuffer().get(indexA), model);
                        Vertex bWorld = enrichWorldVertex(solid.getVertexBuffer().get(indexB), model);
                        Vertex cWorld = enrichWorldVertex(solid.getVertexBuffer().get(indexC), model);

                        Point3D aClip = aWorld.getPosition().mul(mvp);
                        Point3D bClip = bWorld.getPosition().mul(mvp);
                        Point3D cClip = cWorld.getPosition().mul(mvp);
                        List<ClipVertex> poly = new ArrayList<>();
                        poly.add(new ClipVertex(aClip, aWorld.getColor(), aWorld.getUv(), aWorld.getNormal(), aWorld.getWorldPosition()));
                        poly.add(new ClipVertex(bClip, bWorld.getColor(), bWorld.getUv(), bWorld.getNormal(), bWorld.getWorldPosition()));
                        poly.add(new ClipVertex(cClip, cWorld.getColor(), cWorld.getUv(), cWorld.getNormal(), cWorld.getWorldPosition()));
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
                            List<List<Vertex>> viewportTriangles = clipTriangleToViewport(a, b, c);
                            for (List<Vertex> vt : viewportTriangles) {
                                Vertex va = vt.get(0), vb = vt.get(1), vc = vt.get(2);
                                Col ca = displayColor(va.getColor(), solid);
                                Col cb = displayColor(vb.getColor(), solid);
                                Col cc = displayColor(vc.getColor(), solid);
                                Vertex aDraw = new Vertex(new Point3D(va.getX(), va.getY(), va.getZ()), ca, va.getUv(), va.getNormal(), va.getWorldPosition());
                                Vertex bDraw = new Vertex(new Point3D(vb.getX(), vb.getY(), vb.getZ()), cb, vb.getUv(), vb.getNormal(), vb.getWorldPosition());
                                Vertex cDraw = new Vertex(new Point3D(vc.getX(), vc.getY(), vc.getZ()), cc, vc.getUv(), vc.getNormal(), vc.getWorldPosition());
                                triangelRasterizer.rasterize(aDraw, bDraw, cDraw);
                            }
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }
}