package raster;

import java.util.Optional;

public class DepthBuffer implements Raster<Double>{

    private final double[][] buffer;
    private final int width;
    private final int height;

    public DepthBuffer(int width, int height) {
       this.width = width;
       this.height = height;
       this.buffer = new double[height][width];
       clear();
    }

    @Override
    public void setValue(int x, int y, Double value) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }
        buffer[y][x] = value;
    }

    @Override
    public Optional<Double> getValue(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return Optional.empty();
        }
        return Optional.of(buffer[y][x]);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
       return height;
    }

    @Override
    public void clear() {
     for (int i = 0; i < height; i++) {
         for (int j = 0; j < width; j++) {
             buffer[i][j] = 1;
         }
     }
    }
}
