package model;

import transforms.Mat4;

import java.util.List;

public abstract class Solid {

    private final List<Vertex> vertexBuffer;
    private final List<Integer> indexBuffer;
    private final List<SolidPart> partBuffer;
    private final Mat4 modelMat;

    public Solid(final List<Vertex> vertexBuffer, final List<Integer> indexBuffer, final List<SolidPart> partBuffer, final Mat4 modelMat) {

        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
        this.partBuffer = partBuffer;
        this.modelMat = modelMat;


    }
}
