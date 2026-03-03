package renderer;

import model.Vertex;
import raster.ZBuffer;
import rasterize.LineRasterizer;
import rasterize.TriangelRasterizer;
import model.Part;
import solid.Solid;

import javax.swing.*;

import static model.Topology.LINES;
import static model.Topology.TRIANGLES;

public class RendererSolid {
    private LineRasterizer lineRasterizer;
    private TriangelRasterizer triangelRasterizer;

    public RendererSolid(LineRasterizer lineRasterizer, TriangelRasterizer triangelRasterizer) {
        this.lineRasterizer = lineRasterizer;
        this.triangelRasterizer = triangelRasterizer;
    }



    public void render(Solid solid ) {
        //cyklus co projizdi buffer
        for (Part part : solid.getPartBuffer()){
            switch(part.getTopology()){
                case POINTS:
                    //todo points
                    break;
                case LINES:
                    int index = part.getStartIndex();
                    for(int i = 0; i <part.getCount();i++){
                        int indexA = solid.getIndexBuffer().get(index++);
                        int indexB = solid.getIndexBuffer().get(index++);

                        Vertex a = solid.getVertexBuffer().get(index++);
                        Vertex b = solid.getVertexBuffer().get(index++);

                        //todo vrcholy pronasbim MVP

                        //todo orezenani

                        //todo dehomogenizace

                        //todo transormace do okna

                        //rasterizce
                        lineRasterizer.rasterize((int) Math.round(a.getX()),
                                (int) Math.round(a.getY()),
                                (int) Math.round(b.getX()),
                                (int) Math.round(b.getY()));
                    }
                    break;
                case TRIANGLES:
                    int index = part.getStartIndex();
                    for(int i = 0; i <part.getCount();i++){
                        int indexA = solid.getIndexBuffer().get(index++);
                        int indexB = solid.getIndexBuffer().get(index++);
                        int indexC = solid.getIndexBuffer().get(index++);

                        Vertex a = solid.getVertexBuffer().get(index++);
                        Vertex b = solid.getVertexBuffer().get(index++);
                        Vertex c = solid.getVertexBuffer().get(index++);

                        //todo vrcholy pronasbim MVP

                        //todo orezenani

                        //todo dehomogenizace

                        //todo transormace do okna

                        //rasterizce
                        TriangelRasterizer.rasterize((int) Math.round(a.getX()),
                                (int) Math.round(a.getY()),
                                (int) Math.round(b.getX()),
                                (int) Math.round(b.getY()),
                                (int) Math.round(c.getX()),
                                (int) Math.round(c.getY());

                    break;
                    //todo dalsi

            }
        }
    }
}
