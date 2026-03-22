package solid;

import model.Part;
import model.Vertex;
import shader.Shader;
import shader.ShaderConstant;
import shader.ShaderInterpolated;
import transforms.Mat4;
import transforms.Mat4Identity;

import java.util.ArrayList;
import java.util.List;

// zaklad pro vsechna telesa: pole vrcholu, indexy, casti (usecky / trojuhelniky), modelova matice
public abstract class Solid {

    protected final List<Vertex> vertexBuffer;
    protected final List<Integer> indexBuffer;
    protected List<Part> partBuffer;
    private Mat4 modelMat;
    // 0 normalni barva, 1-3 pridat trochu RGB (klavesa C)
    private int colorBlendMode = 0;
    // vychozi barveni = interpolace mezi vrcholy
    protected Shader shader = new ShaderInterpolated();

    public int getColorBlendMode() {
        return colorBlendMode;
    }

    public void setColorBlendMode(int colorBlendMode) {
        this.colorBlendMode = Math.max(0, Math.min(3, colorBlendMode));
    }

    public Solid(final List<Vertex> vertexBuffer,
                 final List<Integer> indexBuffer,
                 final List<Part> partBuffer,
                 final Mat4 modelMat) {

        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
        this.partBuffer = partBuffer;
        // null matice -> jednotkova
        this.modelMat = (modelMat == null) ? new Mat4Identity() : modelMat;
    }

    public List<Vertex> getVertexBuffer() {
        return vertexBuffer;
    }

    public List<Integer> getIndexBuffer() {
        return indexBuffer;
    }

    public List<Part> getPartBuffer() {
        return partBuffer;
    }

    public Mat4 getModelMat() {
        return modelMat;
    }

    public void setModelMat(Mat4 modelMat) {
        this.modelMat = modelMat;
    }

    protected void addIndices(int... indices) {
        for (int i : indices) {
            indexBuffer.add(i);
        }
    }

    public ArrayList<Integer> getIndices() {
        return new ArrayList<>(indexBuffer);
    }

    public Shader getShader() {
        return shader;
    }

    public void setShader(Shader shader) {
        if (shader != null) {
            this.shader = shader;
        }
    }
}
