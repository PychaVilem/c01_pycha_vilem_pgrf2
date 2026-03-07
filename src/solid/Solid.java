package solid;

import model.Part;
import model.Vertex;
import transforms.Mat4;
import transforms.Mat4Identity;

import java.util.ArrayList;
import java.util.List;

public abstract class Solid {

    protected final List<Vertex> vertexBuffer;
    protected final List<Integer> indexBuffer;
    protected List<Part> partBuffer;
    private Mat4 modelMat;

    public Solid(final List<Vertex> vertexBuffer,
                 final List<Integer> indexBuffer,
                 final List<Part> partBuffer,
                 final Mat4 modelMat) {

        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
        this.partBuffer = partBuffer;
        // kdyby někdo poslal null, tak radši identity
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

    /**
     * Přidá do index bufferu libovolný počet indexů.
     */
    protected void addIndices(int... indices) {
        for (int i : indices) {
            indexBuffer.add(i);
        }
    }

    /**
     * Vrátí kopii všech indexů (např. pro renderer).
     */
    public ArrayList<Integer> getIndices() {
        return new ArrayList<>(indexBuffer);
    }
}
