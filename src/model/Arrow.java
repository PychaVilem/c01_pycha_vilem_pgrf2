package model;

import transforms.Mat4Identity;

import java.util.List;

public class Arrow extends Solid {

    public Arrow() {
        super(
                List.of(
                        new Vertex(0, 0, 0.5),
                        new Vertex(200, 0, 0.5)
                ),
                List.of(
                        0, 1
                ),
                List.of(
                        new SolidPart(Topology.LINE_LIST, 1, 0)
                ),
                new Mat4Identity()
        );
    }
        /* 
    public Arrow(){
        super(List.of(
                new Vertex(200,400,0.5)
                ),
                List.of(
                        0,1,2,3,4
                ),
                List.of(
                        new SolidPart(hw),
                        new SolidPart(hw)
                ),
                new Mat4Identity(ver    texBuffer, indexBuffer, partBuffer, modelMat));
    }
     */
}
