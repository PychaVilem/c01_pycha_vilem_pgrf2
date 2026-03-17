package shader;

import model.Vertex;
import transforms.Col;
import transforms.Vec2D;

import java.awt.image.BufferedImage;

/**
 * Shader, který bere barvu z textury podle uv souřadnic ve vrcholu.
 * Pokud uv chybí nebo textura je null, vrátí původní barvu ve vrcholu.
 */
public class ShaderTexture implements Shader {

    private final BufferedImage texture;

    public ShaderTexture(BufferedImage texture) {
        this.texture = texture;
    }

    @Override
    public Col getColor(Vertex pixel) {
        if (texture == null) {
            return pixel.getColor();
        }
        Vec2D uv = pixel.getUv();
        if (uv == null) {
            return pixel.getColor();
        }

        double u = uv.getX();
        double v = uv.getY();

        // základní clamp do [0,1]
        u = Math.max(0.0, Math.min(1.0, u));
        v = Math.max(0.0, Math.min(1.0, v));

        int tx = (int) Math.round(u * (texture.getWidth() - 1));
        int ty = (int) Math.round((1.0 - v) * (texture.getHeight() - 1)); // převrácení Y

        int argb = texture.getRGB(tx, ty);
        return new Col(argb, true);
    }
}

