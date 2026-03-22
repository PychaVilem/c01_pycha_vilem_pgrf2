package model;

// jeden usek index bufferu: od startIndex jsou po sobe usecky nebo trojuhelniky (podle topology)
public class Part {

    private final Topology topology;
    private final int count;
    private final int startIndex;

    public Part(final Topology topology, final int count, final int startIndex){
        this.topology = topology;
        this.count = count;
        this.startIndex = startIndex;
    }

    public int getStartIndex() {
        return startIndex;
    }
    public int getCount() {
        return count;
    }

    public Topology getTopology() {
        return topology;
    }

}
