package rasterize;

import raster.RasterBufferedImage;

import java.awt.*;

// dratovy rezim - usecky pres hotovy Graphics2D drawLine (bez vlastniho z bufferu na carach)
public class LineRasterizerGraphics extends LineRasterizer {

    public LineRasterizerGraphics(RasterBufferedImage raster) {
        super(raster);
    }

    @Override
    public void rasterize(int x1, int y1, int x2, int y2) {
        Graphics g = raster.getImage().getGraphics();
        g.setColor(new Color(color.getRGB()));
        g.drawLine(x1, y1, x2, y2);
    }
}
