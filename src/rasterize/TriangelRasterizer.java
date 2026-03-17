package rasterize;

import model.Vertex;
import raster.ZBuffer;
import shader.Shader;
import shader.ShaderInterpolated;
import transforms.Col;
import util.Lerp;

public class TriangelRasterizer  {
    private final ZBuffer zBuffer;
    private Shader shader = new ShaderInterpolated();
    private int viewportWidth = -1;
    private int viewportHeight = -1;

    public TriangelRasterizer(ZBuffer zBuffer) {
        this.zBuffer = zBuffer;
    }

    /** Nastaví rozměr viewportu pro dokončovací ořezávání (pixely mimo [0,width)x[0,height) se nekreslí). */
    public void setViewport(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    public void setShader(Shader shader) {
        if (shader != null) {
            this.shader = shader;
        }
    }

    public void rasterize(Vertex a, Vertex b, Vertex c) {

        // přepočet souřadnic na int, zůstává double Z
        int ax = (int) Math.round(a.getX());
        int ay = (int) Math.round(a.getY());
        double az = a.getZ();
        Col colA = a.getColor();
        var uvA = a.getUv();

        int bx = (int) Math.round(b.getX());
        int by = (int) Math.round(b.getY());
        double bz = b.getZ();
        Col colB = b.getColor();
        var uvB = b.getUv();

        int cx = (int) Math.round(c.getX());
        int cy = (int) Math.round(c.getY());
        double cz = c.getZ();
        Col colC = c.getColor();
        var uvC = c.getUv();

        // setřídění podle Y: ay <= by <= cy (prohazujeme všechny souřadnice bodu)
        if (ay > by) {
            int tmpX = ax; ax = bx; bx = tmpX;
            int tmpY = ay; ay = by; by = tmpY;
            double tmpZ = az; az = bz; bz = tmpZ;
            Col tmpCol = colA; colA = colB; colB = tmpCol;
            var tmpUv = uvA; uvA = uvB; uvB = tmpUv;
        }
        if (by > cy) {
            int tmpX = bx; bx = cx; cx = tmpX;
            int tmpY = by; by = cy; cy = tmpY;
            double tmpZ = bz; bz = cz; cz = tmpZ;
            Col tmpCol = colB; colB = colC; colC = tmpCol;
            var tmpUv = uvB; uvB = uvC; uvC = tmpUv;
        }
        if (ay > by) {
            int tmpX = ax; ax = bx; bx = tmpX;
            int tmpY = ay; ay = by; by = tmpY;
            double tmpZ = az; az = bz; bz = tmpZ;
            Col tmpCol = colA; colA = colB; colB = tmpCol;
            var tmpUv = uvA; uvA = uvB; uvB = tmpUv;
        }

        // Dokončovací ořezávání: omezení na viewport
        int clipYMin = (viewportHeight > 0) ? 0 : Integer.MIN_VALUE;
        int clipYMax = (viewportHeight > 0) ? viewportHeight - 1 : Integer.MAX_VALUE;
        int clipXMin = (viewportWidth > 0) ? 0 : Integer.MIN_VALUE;
        int clipXMax = (viewportWidth > 0) ? viewportWidth - 1 : Integer.MAX_VALUE;

        // 1. horní část trojúhelníku (A-B proti A-C)
        if (by > ay) {
            int yLo = Math.max(ay, clipYMin);
            int yHi = Math.min(by, clipYMax);
            for (int y = yLo; y <= yHi; y++) {
                // hrana AB
                double tAB = (y - ay) / (double) (by - ay);
                int xAB = (int) Math.round((1 - tAB) * ax + tAB * bx);
                double zAB = (1 - tAB) * az + tAB * bz;
                Col colAB = Lerp.lerp(colA, colB, tAB);
                double uAB = 0, vAB = 0;
                boolean hasUvAB = uvA != null && uvB != null;
                if (hasUvAB) {
                    uAB = Lerp.lerp(uvA.getX(), uvB.getX(), tAB);
                    vAB = Lerp.lerp(uvA.getY(), uvB.getY(), tAB);
                }

                // hrana AC
                double tAC = (y - ay) / (double) (cy - ay);
                int xAC = (int) Math.round((1 - tAC) * ax + tAC * cx);
                double zAC = (1 - tAC) * az + tAC * cz;
                Col colAC = Lerp.lerp(colA, colC, tAC);
                double uAC = 0, vAC = 0;
                boolean hasUvAC = uvA != null && uvC != null;
                if (hasUvAC) {
                    uAC = Lerp.lerp(uvA.getX(), uvC.getX(), tAC);
                    vAC = Lerp.lerp(uvA.getY(), uvC.getY(), tAC);
                }

                int xStart = xAB;
                int xEnd = xAC;
                double zStart = zAB;
                double zEnd = zAC;
                Col colStart = colAB;
                Col colEnd = colAC;
                double uStart = uAB, vStart = vAB;
                double uEnd = uAC, vEnd = vAC;
                boolean hasUvStartEnd = hasUvAB && hasUvAC;

                // kontrola, aby xStart <= xEnd, jinak prohodit
                if (xStart > xEnd) {
                    int tmpX = xStart; xStart = xEnd; xEnd = tmpX;
                    double tmpZ = zStart; zStart = zEnd; zEnd = tmpZ;
                    Col tmpCol = colStart; colStart = colEnd; colEnd = tmpCol;
                    double tmpU = uStart; uStart = uEnd; uEnd = tmpU;
                    double tmpV = vStart; vStart = vEnd; vEnd = tmpV;
                }

                // ořezání scanline na viewport
                int xLo = Math.max(xStart, clipXMin);
                int xHi = Math.min(xEnd, clipXMax);
                if (xLo > xHi) continue;

                int dx = xEnd - xStart;
                for (int x = xLo; x <= xHi; x++) {
                    double t = (dx == 0) ? 0.0 : (x - xStart) / (double) dx;
                    double finalZ = (1 - t) * zStart + t * zEnd;
                    Col interpolated = Lerp.lerp(colStart, colEnd, t);
                    var uvPixel = hasUvStartEnd
                            ? new transforms.Vec2D(
                                    Lerp.lerp(uStart, uEnd, t),
                                    Lerp.lerp(vStart, vEnd, t)
                            )
                            : null;
                    Vertex pixel = new Vertex(x, y, finalZ, interpolated, uvPixel);
                    Col shaded = shader.getColor(pixel);
                    zBuffer.setPixelWithZTest(x, y, finalZ, shaded);
                }
            }
        }

        // 2. dolní část trojúhelníku (B-C proti A-C)
        if (cy > by) {
            int yLo = Math.max(by + 1, clipYMin);
            int yHi = Math.min(cy, clipYMax);
            for (int y = yLo; y <= yHi; y++) {
                // hrana BC
                double tBC = (y - by) / (double) (cy - by);
                int xBC = (int) Math.round((1 - tBC) * bx + tBC * cx);
                double zBC = (1 - tBC) * bz + tBC * cz;
                Col colBC = Lerp.lerp(colB, colC, tBC);
                double uBC = 0, vBC = 0;
                boolean hasUvBC = uvB != null && uvC != null;
                if (hasUvBC) {
                    uBC = Lerp.lerp(uvB.getX(), uvC.getX(), tBC);
                    vBC = Lerp.lerp(uvB.getY(), uvC.getY(), tBC);
                }

                // hrana AC (stejná jako výše)
                double tAC = (y - ay) / (double) (cy - ay);
                int xAC = (int) Math.round((1 - tAC) * ax + tAC * cx);
                double zAC = (1 - tAC) * az + tAC * cz;
                Col colAC = Lerp.lerp(colA, colC, tAC);
                double uAC = 0, vAC = 0;
                boolean hasUvAC = uvA != null && uvC != null;
                if (hasUvAC) {
                    uAC = Lerp.lerp(uvA.getX(), uvC.getX(), tAC);
                    vAC = Lerp.lerp(uvA.getY(), uvC.getY(), tAC);
                }

                int xStart = xBC;
                int xEnd = xAC;
                double zStart = zBC;
                double zEnd = zAC;
                Col colStart = colBC;
                Col colEnd = colAC;
                double uStart = uBC, vStart = vBC;
                double uEnd = uAC, vEnd = vAC;
                boolean hasUvStartEnd = hasUvBC && hasUvAC;

                if (xStart > xEnd) {
                    int tmpX = xStart; xStart = xEnd; xEnd = tmpX;
                    double tmpZ = zStart; zStart = zEnd; zEnd = tmpZ;
                    Col tmpCol = colStart; colStart = colEnd; colEnd = tmpCol;
                    double tmpU = uStart; uStart = uEnd; uEnd = tmpU;
                    double tmpV = vStart; vStart = vEnd; vEnd = tmpV;
                }

                int xLo = Math.max(xStart, clipXMin);
                int xHi = Math.min(xEnd, clipXMax);
                if (xLo > xHi) continue;

                int dx = xEnd - xStart;
                for (int x = xLo; x <= xHi; x++) {
                    double t = (dx == 0) ? 0.0 : (x - xStart) / (double) dx;
                    double finalZ = (1 - t) * zStart + t * zEnd;
                    Col interpolated = Lerp.lerp(colStart, colEnd, t);
                    var uvPixel = hasUvStartEnd
                            ? new transforms.Vec2D(
                                    Lerp.lerp(uStart, uEnd, t),
                                    Lerp.lerp(vStart, vEnd, t)
                            )
                            : null;
                    Vertex pixel = new Vertex(x, y, finalZ, interpolated, uvPixel);
                    Col shaded = shader.getColor(pixel);
                    zBuffer.setPixelWithZTest(x, y, finalZ, shaded);
                }
            }
        }
    }
}