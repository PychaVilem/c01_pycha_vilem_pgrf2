package shader;

import model.Vertex;
import transforms.Col;

public class ShaderConstant  implements  Shader{

    @Override
    public Col getColor(Vertex pixel) {
        return new Col(0xff0000);
    }
}
