package rasterize;

import model.Vertex;
import raster.ZBuffer;
import transforms.Col;

public class TriangelRasterizer  {
    private final ZBuffer zBuffer;

    public TriangelRasterizer(ZBuffer zBuffer) {
        this.zBuffer = zBuffer;

    }

    public void rasterize(Vertex a, Vertex b, Vertex c) {
        // přepočet souřadnic na int, zůstává double Z
        int ax = (int) Math.round(a.getX());
        int ay = (int) Math.round(a.getY());
        double az = a.getZ();
        Col colA = a.getColor();

        int bx = (int) Math.round(b.getX());
        int by = (int) Math.round(b.getY());
        double bz = b.getZ();
        Col colB = b.getColor();

        int cx = (int) Math.round(c.getX());
        int cy = (int) Math.round(c.getY());
        double cz = c.getZ();
        Col colC = c.getColor();

        // setřídění podle Y: ay <= by <= cy (prohazujeme všechny souřadnice bodu)
        if (ay > by) {
            int tmpX = ax; ax = bx; bx = tmpX;
            int tmpY = ay; ay = by; by = tmpY;
            double tmpZ = az; az = bz; bz = tmpZ;
            Col tmpCol = colA; colA = colB; colB = tmpCol;
        }
        if (by > cy) {
            int tmpX = bx; bx = cx; cx = tmpX;
            int tmpY = by; by = cy; cy = tmpY;
            double tmpZ = bz; bz = cz; cz = tmpZ;
            Col tmpCol = colB; colB = colC; colC = tmpCol;
        }
        if (ay > by) {
            int tmpX = ax; ax = bx; bx = tmpX;
            int tmpY = ay; ay = by; by = tmpY;
            double tmpZ = az; az = bz; bz = tmpZ;
            Col tmpCol = colA; colA = colB; colB = tmpCol;
        }

        // 1. horní část trojúhelníku (A-B proti A-C)
        if (by > ay) {
            for (int y = ay; y <= by; y++) {
                // hrana AB
                double tAB = (y - ay) / (double) (by - ay);
                int xAB = (int) Math.round((1 - tAB) * ax + tAB * bx);
                double zAB = (1 - tAB) * az + tAB * bz;

                // hrana AC
                double tAC = (y - ay) / (double) (cy - ay);
                int xAC = (int) Math.round((1 - tAC) * ax + tAC * cx);
                double zAC = (1 - tAC) * az + tAC * cz;

                int xStart = xAB;
                int xEnd = xAC;
                double zStart = zAB;
                double zEnd = zAC;

                // kontrola, aby xStart <= xEnd, jinak prohodit
                if (xStart > xEnd) {
                    int tmpX = xStart; xStart = xEnd; xEnd = tmpX;
                    double tmpZ = zStart; zStart = zEnd; zEnd = tmpZ;
                }

                // scanline od xStart do xEnd včetně
                int dx = xEnd - xStart;
                for (int x = xStart; x <= xEnd; x++) {
                    double t = (dx == 0) ? 0.0 : (x - xStart) / (double) dx;
                    double finalZ = (1 - t) * zStart + t * zEnd;
                    // zatím použijeme barvu vrcholu A (můžeš později interpolovat barvu podobně jako Z)
                    zBuffer.setPixelWithZTest(x, y, finalZ, colA);
                }
            }
        }

        // 2. dolní část trojúhelníku (B-C proti A-C)
        if (cy > by) {
            for (int y = by + 1; y <= cy; y++) {
                // hrana BC
                double tBC = (y - by) / (double) (cy - by);
                int xBC = (int) Math.round((1 - tBC) * bx + tBC * cx);
                double zBC = (1 - tBC) * bz + tBC * cz;

                // hrana AC (stejná jako výše)
                double tAC = (y - ay) / (double) (cy - ay);
                int xAC = (int) Math.round((1 - tAC) * ax + tAC * cx);
                double zAC = (1 - tAC) * az + tAC * cz;

                int xStart = xBC;
                int xEnd = xAC;
                double zStart = zBC;
                double zEnd = zAC;

                if (xStart > xEnd) {
                    int tmpX = xStart; xStart = xEnd; xEnd = tmpX;
                    double tmpZ = zStart; zStart = zEnd; zEnd = tmpZ;
                }

                int dx = xEnd - xStart;
                for (int x = xStart; x <= xEnd; x++) {
                    double t = (dx == 0) ? 0.0 : (x - xStart) / (double) dx;
                    double finalZ = (1 - t) * zStart + t * zEnd;
                    zBuffer.setPixelWithZTest(x, y, finalZ, colA);
                }
            }
        }
    }
}
//zBuffer
// prijme barvu -> refaktor
//
//