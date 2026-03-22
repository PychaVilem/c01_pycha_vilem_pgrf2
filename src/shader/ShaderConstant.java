package shader;

import model.Vertex;
import transforms.Col;

// jedna barva na cely objekt - phong pak pouzije tuto barvu jako "material" vsude stejne
public class ShaderConstant implements Shader {

    private Col color;

    public ShaderConstant() {
        this(new Col(0xff0000));
    }

    public ShaderConstant(Col color) {
        this.color = color != null ? new Col(color) : new Col(0xff0000);
    }

    public Col getSolidColor() {
        return new Col(color);
    }

    public void setSolidColor(Col color) {
        if (color != null) {
            this.color = new Col(color);
        }
    }

    @Override
    public Col getColor(Vertex pixel) {
        return new Col(color);
    }
}
