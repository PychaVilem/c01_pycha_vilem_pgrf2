package shader;

import model.Vertex;
import transforms.Col;

@FunctionalInterface
public interface Shader {
    Col getColor(Vertex pixel);
}
