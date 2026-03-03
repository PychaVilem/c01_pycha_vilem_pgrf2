package solid;

import model.Part;
import model.Topology;
import model.Vertex;

public class Arrow extends Solid {

    public Arrow() {

        //todo az bude cely ceretezc nechci souradnice ve screen space
        vertexBuffer.add(new Vertex(200,300,0.5));
        vertexBuffer.add(new Vertex(250,300,0.5));
        vertexBuffer.add(new Vertex(250,320,0.5,new Col(0xff0000)));
        vertexBuffer.add(new Vertex(270,300,0.5));
        vertexBuffer.add(new Vertex(250,250,0.5));

        addIndices(0,1); //Lines
        addIndices(4,3,2); //Triangles

        partBuffer.add(new Part(Topology.LINES,0,1));
        partBuffer.add(new Part(Topology.TRIANGLES,2,1));
    }

}
