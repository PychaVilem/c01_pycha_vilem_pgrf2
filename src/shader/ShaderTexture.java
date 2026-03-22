package shader;

import model.Vertex;
import transforms.Col;
import transforms.Vec2D;

import java.awt.image.BufferedImage;

// Textura: pixelovy shader bere barvu z BufferedImage podle interpolovanych u,v.
// u,v jsou v rozsahu cca 0..1 (nekdy z modelu vic - proto clamp).
// getRGB z obrazku je v java konvenci; prevod na Col(argb, true).
// Kdyz neni textura nebo uv, spadnu na barvu vrcholu z trojuhelniku (gouraud zaloha).
public class ShaderTexture implements Shader {

    private final BufferedImage texture;

    public ShaderTexture(BufferedImage texture) {
        this.texture = texture;
    }

    public BufferedImage getTexture() {
        return texture;
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

        u = Math.max(0.0, Math.min(1.0, u));
        v = Math.max(0.0, Math.min(1.0, v));

        int tx = (int) Math.round(u * (texture.getWidth() - 1));
        // obraz ma y dolu, moje v nekdy opacne
        int ty = (int) Math.round((1.0 - v) * (texture.getHeight() - 1));

        int argb = texture.getRGB(tx, ty);
        return new Col(argb, true);
    }
}

