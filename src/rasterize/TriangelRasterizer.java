package rasterize;

import model.Vertex;
import raster.ZBuffer;
import shader.Shader;
import shader.ShaderInterpolated;
import transforms.Col;
import transforms.Point3D;
import transforms.Vec3D;
import util.Lerp;

// ============================================================================
// JAK FUNGUJE TRIANGEL RASTERIZER (scanline vyplnovani trojuhelniku)
// ============================================================================
// Vstup uz dostanes z RendererSolid: trojuhelnik je v 2D obrazovce (x,y v pixelech),
// z v kazdem vrcholu je hloubka (pro z-buffer). Ukolem je najit vsechny pixely uvnitr
// trojuhelniku a u kazdeho dopocitat interpolovane hodnoty (barva, z, uv, normala, ...).
//
// PROC DVA CYKLY PODLE Y (horni pas + dolni pas)
// ----------------------------------------------
// Tri vrcholy seradim podle y: po tri prohozenich plati ay <= by <= cy.
// Oznacim je mentalne A=horni, B=prostredni podle y, C=spodni (nemusi odpovidat pismenum a,b,c z parametru).
// Konvexni trojuhelnik rozdelim vodorovnou predstavou prochazejici urovni B na:
//   - horni cast trojuhelniku: vyplnuji radky y od ay do by, pruseciky vodorovne cary s hranami AB a AC
//   - dolni cast: radky y od by+1 do cy, pruseciky s hranami BC a AC
// Radek y=by pocitam jen v hornim cyklu; v dolnim zacinam by+1 aby se radek u urovne B nespocital dvakrat.
//
// JEDEN RADEK Y (co se deje uvnitr for y)
// -------------------------------------
// Pro danou vodorovnou caru y spocitam parametrem t kde usecka "stoji" na kazde hrane:
//   t = (y - y_dole) / (y_nahore - y_dole)  ... vlastne mezi dvema vrcholy hrany
// Z t pak x, z, barva, uv, normala, world stejnym t (linearni interpolace podle hrany).
// Dostanu dva body na levom a pravom okraji trojuhelniku na tom radku -> xStart, xEnd.
// Kdyz xStart > xEnd, prohodim (abych sel vzdy zleva doprava).
// Orezu x na [0, width-1] kvuli oknu.
//
// VNITRNI SMYCKA PODLE X
// ----------------------
// Na usecce mezi (xStart) a (xEnd) dalsi t2 = (x - xStart) / (xEnd - xStart) a lerp vseho mezi levym a pravym
// okrajem scanline - to je interpolace vodorovne uvnitr trojuhelniku (gouraud pro barvu atd.).
//
// SHADER A Z-BUFFER
// -----------------
// Vertex pro "fragment" = pixel: (x, y, finalZ) + interpolovane atributy -> shader.getColor(pixel).
// setPixelWithZTest: zapise barvu jen kdyz je finalZ mensi nez dosavadni hloubka (coz je bliz kamine).
//
// OMEZENI
// ---------
// Interpolace je v obrazovce linearni - neni perspektivne korektni (spravne by se delilo w).
// ============================================================================
public class TriangelRasterizer  {
    private final ZBuffer zBuffer;
    private Shader shader = new ShaderInterpolated();
    // rozmery okna pro orez pixelu pri rasterizaci (-1 = neorezavat podle okna)
    private int viewportWidth = -1;
    private int viewportHeight = -1;

    public TriangelRasterizer(ZBuffer zBuffer) {
        this.zBuffer = zBuffer;
    }

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
        // Pozor: promenne ax,ay,... po razeni uz neodpovidaji puvodnim a,b,c - vzdycky plati ay<=by<=cy.

        // Vrchol = souradnice v obrazovce (zaokrouhlene x,y), z = hloubka pro Z-buffer, zbytek = atributy k interpolaci
        int ax = (int) Math.round(a.getX());
        int ay = (int) Math.round(a.getY());
        double az = a.getZ();
        Col colA = a.getColor();
        var uvA = a.getUv();
        var nA = a.getNormal();
        var wpA = a.getWorldPosition();

        int bx = (int) Math.round(b.getX());
        int by = (int) Math.round(b.getY());
        double bz = b.getZ();
        Col colB = b.getColor();
        var uvB = b.getUv();
        var nB = b.getNormal();
        var wpB = b.getWorldPosition();

        int cx = (int) Math.round(c.getX());
        int cy = (int) Math.round(c.getY());
        double cz = c.getZ();
        Col colC = c.getColor();
        var uvC = c.getUv();
        var nC = c.getNormal();
        var wpC = c.getWorldPosition();

        // Bubble-like razeni: po trech pruchodech je ay <= by <= cy (nejmensi y nahore obrazovky)
        if (ay > by) {
            int tmpX = ax; ax = bx; bx = tmpX;
            int tmpY = ay; ay = by; by = tmpY;
            double tmpZ = az; az = bz; bz = tmpZ;
            Col tmpCol = colA; colA = colB; colB = tmpCol;
            var tmpUv = uvA; uvA = uvB; uvB = tmpUv;
            var tmpN = nA; nA = nB; nB = tmpN;
            var tmpWp = wpA; wpA = wpB; wpB = tmpWp;
        }
        if (by > cy) {
            int tmpX = bx; bx = cx; cx = tmpX;
            int tmpY = by; by = cy; cy = tmpY;
            double tmpZ = bz; bz = cz; cz = tmpZ;
            Col tmpCol = colB; colB = colC; colC = tmpCol;
            var tmpUv = uvB; uvB = uvC; uvC = tmpUv;
            var tmpN = nB; nB = nC; nC = tmpN;
            var tmpWp = wpB; wpB = wpC; wpC = tmpWp;
        }
        if (ay > by) {
            int tmpX = ax; ax = bx; bx = tmpX;
            int tmpY = ay; ay = by; by = tmpY;
            double tmpZ = az; az = bz; bz = tmpZ;
            Col tmpCol = colA; colA = colB; colB = tmpCol;
            var tmpUv = uvA; uvA = uvB; uvB = tmpUv;
            var tmpN = nA; nA = nB; nB = tmpN;
            var tmpWp = wpA; wpA = wpB; wpB = tmpWp;
        }

        // orez na platno - mimo rozsah se vubec nekresli
        int clipYMin = (viewportHeight > 0) ? 0 : Integer.MIN_VALUE;
        int clipYMax = (viewportHeight > 0) ? viewportHeight - 1 : Integer.MAX_VALUE;
        int clipXMin = (viewportWidth > 0) ? 0 : Integer.MIN_VALUE;
        int clipXMax = (viewportWidth > 0) ? viewportWidth - 1 : Integer.MAX_VALUE;

        // ----- HORNI PAS: y od nejmensiho y az po prostredni vrchol (by) -----
        // Na kazdem radku hledam prusecik vodorovne cary s hranou AB a s hranou AC (spolecny vrchol A).
        if (by > ay) {
            int yLo = Math.max(ay, clipYMin);
            int yHi = Math.min(by, clipYMax);
            for (int y = yLo; y <= yHi; y++) {
                // t = jak daleko po hrane AB jsem od A k B v ose y (0 = u A, 1 = u B)
                double tAB = (y - ay) / (double) (by - ay);
                int xAB = (int) Math.round((1 - tAB) * ax + tAB * bx);
                double zAB = (1 - tAB) * az + tAB * bz;
                Col colAB = Lerp.lerp(colA, colB, tAB);
                double uAB = 0, vAB = 0;
                boolean hasUvAB = uvA != null && uvB != null;
                boolean hasNAB = nA != null && nB != null;
                boolean hasWpAB = wpA != null && wpB != null;
                if (hasUvAB) {
                    uAB = Lerp.lerp(uvA.getX(), uvB.getX(), tAB);
                    vAB = Lerp.lerp(uvA.getY(), uvB.getY(), tAB);
                }
                Vec3D nAB = hasNAB
                        ? new Vec3D(
                        Lerp.lerp(nA.getX(), nB.getX(), tAB),
                        Lerp.lerp(nA.getY(), nB.getY(), tAB),
                        Lerp.lerp(nA.getZ(), nB.getZ(), tAB)
                ).normalized().orElse(null)
                        : null;
                Point3D wpAB = hasWpAB
                        ? new Point3D(
                        Lerp.lerp(wpA.getX(), wpB.getX(), tAB),
                        Lerp.lerp(wpA.getY(), wpB.getY(), tAB),
                        Lerp.lerp(wpA.getZ(), wpB.getZ(), tAB)
                )
                        : null;

                // totez pro hranu AC (od A ke C) - na tomhle radku y je bod na "prave" strane trojuhelniku
                double tAC = (y - ay) / (double) (cy - ay);
                int xAC = (int) Math.round((1 - tAC) * ax + tAC * cx);
                double zAC = (1 - tAC) * az + tAC * cz;
                Col colAC = Lerp.lerp(colA, colC, tAC);
                double uAC = 0, vAC = 0;
                boolean hasUvAC = uvA != null && uvC != null;
                boolean hasNAC = nA != null && nC != null;
                boolean hasWpAC = wpA != null && wpC != null;
                if (hasUvAC) {
                    uAC = Lerp.lerp(uvA.getX(), uvC.getX(), tAC);
                    vAC = Lerp.lerp(uvA.getY(), uvC.getY(), tAC);
                }
                Vec3D nAC = hasNAC
                        ? new Vec3D(
                        Lerp.lerp(nA.getX(), nC.getX(), tAC),
                        Lerp.lerp(nA.getY(), nC.getY(), tAC),
                        Lerp.lerp(nA.getZ(), nC.getZ(), tAC)
                ).normalized().orElse(null)
                        : null;
                Point3D wpAC = hasWpAC
                        ? new Point3D(
                        Lerp.lerp(wpA.getX(), wpC.getX(), tAC),
                        Lerp.lerp(wpA.getY(), wpC.getY(), tAC),
                        Lerp.lerp(wpA.getZ(), wpC.getZ(), tAC)
                )
                        : null;

                int xStart = xAB;
                int xEnd = xAC;
                double zStart = zAB;
                double zEnd = zAC;
                Col colStart = colAB;
                Col colEnd = colAC;
                double uStart = uAB, vStart = vAB;
                double uEnd = uAC, vEnd = vAC;
                boolean hasUvStartEnd = hasUvAB && hasUvAC;
                Vec3D nStart = nAB;
                Vec3D nEnd = nAC;
                Point3D wpStart = wpAB;
                Point3D wpEnd = wpAC;

                // Na radku y jsou dva pruseciky; xStart/xEnd jsou levy a pravy okraj vyplne (musi byt xStart <= xEnd)
                if (xStart > xEnd) {
                    int tmpX = xStart; xStart = xEnd; xEnd = tmpX;
                    double tmpZ = zStart; zStart = zEnd; zEnd = tmpZ;
                    Col tmpCol = colStart; colStart = colEnd; colEnd = tmpCol;
                    double tmpU = uStart; uStart = uEnd; uEnd = tmpU;
                    double tmpV = vStart; vStart = vEnd; vEnd = tmpV;
                    Vec3D tmpN = nStart; nStart = nEnd; nEnd = tmpN;
                    Point3D tmpWp = wpStart; wpStart = wpEnd; wpEnd = tmpWp;
                }

                int xLo = Math.max(xStart, clipXMin);
                int xHi = Math.min(xEnd, clipXMax);
                if (xLo > xHi) continue;

                // Projdu vsechny pixely na tom radku mezi levym a pravym okrajem trojuhelniku
                int dx = xEnd - xStart;
                for (int x = xLo; x <= xHi; x++) {
                    // t2 = pozice na usecce od xStart do xEnd (0 = levy okraj scanline, 1 = pravy)
                    double t = (dx == 0) ? 0.0 : (x - xStart) / (double) dx;
                    double finalZ = (1 - t) * zStart + t * zEnd;
                    Col interpolated = Lerp.lerp(colStart, colEnd, t);
                    var uvPixel = hasUvStartEnd
                            ? new transforms.Vec2D(
                                    Lerp.lerp(uStart, uEnd, t),
                                    Lerp.lerp(vStart, vEnd, t)
                            )
                            : null;
                    Vec3D nPixel = (nStart != null && nEnd != null)
                            ? new Vec3D(
                            Lerp.lerp(nStart.getX(), nEnd.getX(), t),
                            Lerp.lerp(nStart.getY(), nEnd.getY(), t),
                            Lerp.lerp(nStart.getZ(), nEnd.getZ(), t)
                    ).normalized().orElse(null)
                            : null;
                    Point3D wpPixel = (wpStart != null && wpEnd != null)
                            ? new Point3D(
                            Lerp.lerp(wpStart.getX(), wpEnd.getX(), t),
                            Lerp.lerp(wpStart.getY(), wpEnd.getY(), t),
                            Lerp.lerp(wpStart.getZ(), wpEnd.getZ(), t)
                    )
                            : null;
                    Vertex pixel = new Vertex(new Point3D(x, y, finalZ), interpolated, uvPixel, nPixel, wpPixel);
                    Col shaded = shader.getColor(pixel);
                    zBuffer.setPixelWithZTest(x, y, finalZ, shaded);
                }
            }
        }

        // ----- DOLNI PAS: y od (by+1) do nejvetsiho y -----
        // Spodni cast trojuhelniku: hrany BC a AC (spolecny "hrot" rozdeleni je u B v ose y).
        // by+1: radek na urovni B uz osetril horni cyklus (jinak by se pixely na hrane B pocitaly dvakrat)
        if (cy > by) {
            int yLo = Math.max(by + 1, clipYMin);
            int yHi = Math.min(cy, clipYMax);
            for (int y = yLo; y <= yHi; y++) {
                double tBC = (y - by) / (double) (cy - by);
                int xBC = (int) Math.round((1 - tBC) * bx + tBC * cx);
                double zBC = (1 - tBC) * bz + tBC * cz;
                Col colBC = Lerp.lerp(colB, colC, tBC);
                double uBC = 0, vBC = 0;
                boolean hasUvBC = uvB != null && uvC != null;
                boolean hasNBC = nB != null && nC != null;
                boolean hasWpBC = wpB != null && wpC != null;
                if (hasUvBC) {
                    uBC = Lerp.lerp(uvB.getX(), uvC.getX(), tBC);
                    vBC = Lerp.lerp(uvB.getY(), uvC.getY(), tBC);
                }
                Vec3D nBC = hasNBC
                        ? new Vec3D(
                        Lerp.lerp(nB.getX(), nC.getX(), tBC),
                        Lerp.lerp(nB.getY(), nC.getY(), tBC),
                        Lerp.lerp(nB.getZ(), nC.getZ(), tBC)
                ).normalized().orElse(null)
                        : null;
                Point3D wpBC = hasWpBC
                        ? new Point3D(
                        Lerp.lerp(wpB.getX(), wpC.getX(), tBC),
                        Lerp.lerp(wpB.getY(), wpC.getY(), tBC),
                        Lerp.lerp(wpB.getZ(), wpC.getZ(), tBC)
                )
                        : null;

                // Druhy okraj scanline: porad hrana AC (A je porad nejvyssi, C nejnizsi) - stejna vzorec jako nahore
                double tAC = (y - ay) / (double) (cy - ay);
                int xAC = (int) Math.round((1 - tAC) * ax + tAC * cx);
                double zAC = (1 - tAC) * az + tAC * cz;
                Col colAC = Lerp.lerp(colA, colC, tAC);
                double uAC = 0, vAC = 0;
                boolean hasUvAC = uvA != null && uvC != null;
                boolean hasNAC = nA != null && nC != null;
                boolean hasWpAC = wpA != null && wpC != null;
                if (hasUvAC) {
                    uAC = Lerp.lerp(uvA.getX(), uvC.getX(), tAC);
                    vAC = Lerp.lerp(uvA.getY(), uvC.getY(), tAC);
                }
                Vec3D nAC = hasNAC
                        ? new Vec3D(
                        Lerp.lerp(nA.getX(), nC.getX(), tAC),
                        Lerp.lerp(nA.getY(), nC.getY(), tAC),
                        Lerp.lerp(nA.getZ(), nC.getZ(), tAC)
                ).normalized().orElse(null)
                        : null;
                Point3D wpAC = hasWpAC
                        ? new Point3D(
                        Lerp.lerp(wpA.getX(), wpC.getX(), tAC),
                        Lerp.lerp(wpA.getY(), wpC.getY(), tAC),
                        Lerp.lerp(wpA.getZ(), wpC.getZ(), tAC)
                )
                        : null;

                int xStart = xBC;
                int xEnd = xAC;
                double zStart = zBC;
                double zEnd = zAC;
                Col colStart = colBC;
                Col colEnd = colAC;
                double uStart = uBC, vStart = vBC;
                double uEnd = uAC, vEnd = vAC;
                boolean hasUvStartEnd = hasUvBC && hasUvAC;
                Vec3D nStart = nBC;
                Vec3D nEnd = nAC;
                Point3D wpStart = wpBC;
                Point3D wpEnd = wpAC;

                if (xStart > xEnd) {
                    int tmpX = xStart; xStart = xEnd; xEnd = tmpX;
                    double tmpZ = zStart; zStart = zEnd; zEnd = tmpZ;
                    Col tmpCol = colStart; colStart = colEnd; colEnd = tmpCol;
                    double tmpU = uStart; uStart = uEnd; uEnd = tmpU;
                    double tmpV = vStart; vStart = vEnd; vEnd = tmpV;
                    Vec3D tmpN = nStart; nStart = nEnd; nEnd = tmpN;
                    Point3D tmpWp = wpStart; wpStart = wpEnd; wpEnd = tmpWp;
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
                    Vec3D nPixel = (nStart != null && nEnd != null)
                            ? new Vec3D(
                            Lerp.lerp(nStart.getX(), nEnd.getX(), t),
                            Lerp.lerp(nStart.getY(), nEnd.getY(), t),
                            Lerp.lerp(nStart.getZ(), nEnd.getZ(), t)
                    ).normalized().orElse(null)
                            : null;
                    Point3D wpPixel = (wpStart != null && wpEnd != null)
                            ? new Point3D(
                            Lerp.lerp(wpStart.getX(), wpEnd.getX(), t),
                            Lerp.lerp(wpStart.getY(), wpEnd.getY(), t),
                            Lerp.lerp(wpStart.getZ(), wpEnd.getZ(), t)
                    )
                            : null;
                    Vertex pixel = new Vertex(new Point3D(x, y, finalZ), interpolated, uvPixel, nPixel, wpPixel);
                    Col shaded = shader.getColor(pixel);
                    zBuffer.setPixelWithZTest(x, y, finalZ, shaded);
                }
            }
        }
    }
}