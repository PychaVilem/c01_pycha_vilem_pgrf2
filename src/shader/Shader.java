package shader;

import model.Vertex;
import transforms.Col;

// Shader: jedna metoda getColor(vertex) kde "vertex" reprezentuje jeden pixel uvnitr trojuhelniku
// (ma x,y v pixelech, z, interpolovanou barvu/uv/normalu/world - podle co rasterizer naplnil).
// Daji se psat lambda nebo tridy (ShaderLit obali dalsi shader).
@FunctionalInterface
public interface Shader {
    Col getColor(Vertex pixel);
}
