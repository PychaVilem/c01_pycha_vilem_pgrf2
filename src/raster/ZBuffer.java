package raster;

import transforms.Col;

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
    //vzdy budu volat tuhle metodu
    public void setPixelWithZTest(int x, int y, double z, Col color) {
        // nactu hodnotu z depth bufferu
        var currentDepthOpt = depthBuffer.getValue(x, y);
        double currentDepth = currentDepthOpt.orElse(1.0);

        // porovnam hodnotu s hodnout Z, ktera prisla do metody
        // mensi Z je bliz k "kamere"
        if (z < currentDepth) {
            // obarvim pixel a updatuju depth buffer
            imageBuffer.setValue(x, y, color);
            depthBuffer.setValue(x, y, z);
        }
    }
}
