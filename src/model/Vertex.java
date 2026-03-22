package model;

import transforms.Col;
import transforms.Point3D;
import transforms.Vec2D;
import transforms.Vec3D;

// jeden vrchol: pozice v modelu, barva, volitelne uv pro texturu
// normala a world pozice se doplni az v rendereru (po vynasobeni modelovou matici)
public class Vertex {

    private final Point3D position;
    private final Col color;
    private final Vec2D uv;
    private final Vec3D normal;
    private final Point3D worldPosition;

    public Vertex(double x, double y, double z) {
        this(new Point3D(x, y, z), new Col(0xffffff), null, null, null);
    }

    public Vertex(double x, double y, double z, Col color) {
        this(new Point3D(x, y, z), color, null, null, null);
    }

    public Vertex(double x, double y, double z, Col color, Vec2D uv) {
        this(new Point3D(x, y, z), color, uv, null, null);
    }

    public Vertex(double x, double y, double z, Col color, Vec2D uv, Vec3D normal) {
        this(new Point3D(x, y, z), color, uv, normal, null);
    }

    public Vertex(Point3D position, Col color) {
        this(position, color, null, null, null);
    }

    public Vertex(Point3D position, Col color, Vec2D uv) {
        this(position, color, uv, null, null);
    }

    public Vertex(Point3D position, Col color, Vec2D uv, Vec3D normal) {
        this(position, color, uv, normal, position);
    }

    public Vertex(Point3D position, Col color, Vec2D uv, Vec3D normal, Point3D worldPosition) {
        this.position = position;
        this.color = color != null ? color : new Col(0xffffff);
        this.uv = uv;
        this.normal = normal;
        this.worldPosition = worldPosition;
    }

    public Point3D getPosition() {
        return position;
    }

    public Col getColor() {
        return color;
    }

    public Vec2D getUv() {
        return uv;
    }

    public Vec3D getNormal() {
        return normal;
    }

    public Point3D getWorldPosition() {
        return worldPosition;
    }

    public double getX() {
        return position.getX();
    }

    public double getY() {
        return position.getY();
    }

    public double getZ() {
        return position.getZ();
    }
}
