package rasterize;

import model.Line;
import raster.RasterBufferedImage;
import transforms.Col;

public abstract class LineRasterizer {
    protected RasterBufferedImage raster;
    protected Col color;

    public LineRasterizer(RasterBufferedImage raster) {
        this.raster = raster;
        this.color = new Col(0xffffff);
    }

    public void rasterize(int x1, int y1, int x2, int y2) {

    }

    public void rasterize(Line line) {
        rasterize(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    public void setColor(Col color) {
        this.color = color;
    }
}
