package model;

public class SolidPart {

    private final Topology topology;
    private final int primitveCount;
    private final int startIndex;

    public SolidPart(final Topology topology, final int primitiveCount, final int startIndex){
        this.topology = topology;
        this.primitveCount = primitiveCount;
        this.startIndex = startIndex;
    }
}
