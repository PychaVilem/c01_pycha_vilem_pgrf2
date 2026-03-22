package raster;

import transforms.Col;

// spojuje obrazovy raster s depth bufferem - pri kresleni pixelu se nejdriv koukne na Z
public class ZBuffer {
    private final Raster<Col> imageBuffer;
    private final Raster<Double> depthBuffer;

    public ZBuffer(Raster<Col> imageBuffer) {
        this.imageBuffer = imageBuffer;
        this.depthBuffer = new DepthBuffer(imageBuffer.getWidth(), imageBuffer.getHeight());

    }

    public void clear() {
        depthBuffer.clear();
    }

    public void setPixelWithZTest(int x, int y, double z, Col color) {
        var currentDepthOpt = depthBuffer.getValue(x, y);
        double currentDepth = currentDepthOpt.orElse(1.0);

        // mensi z = bliz, prepisu jen kdyz je novy fragment pred starsim
        if (z < currentDepth) {
            imageBuffer.setValue(x, y, color);
            depthBuffer.setValue(x, y, z);
        }
    }
}
