package raster;

import transforms.Col;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Optional;

// bitmapa pro vykresleni sceny (barva per pixel pres Col)
public class RasterBufferedImage implements Raster<Col> {

    private BufferedImage image;

    public RasterBufferedImage(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public void setValue(int x, int y, Col color) {
        // mimo platno nic neukladam
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight() || color == null) {
            return;
        }
        image.setRGB(x, y, color.getRGB());
    }

    @Override
    public Optional<Col> getValue(int x, int y) {
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return Optional.empty();
        }
        return Optional.of(new Col(image.getRGB(x, y)));
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public void clear() {
        Graphics g = image.getGraphics();
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
    }

    public BufferedImage getImage() {
        return image;
    }
}
