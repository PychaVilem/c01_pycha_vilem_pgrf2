package shader;

import model.Vertex;
import transforms.Col;

// gouraud: barva uz je v pixelu interpolovana v rasterizacnim kroku z vrcholu trojuhelniku
public class ShaderInterpolated implements Shader {

    @Override
    public Col getColor(Vertex pixel) {
        return pixel.getColor();
    }
}
