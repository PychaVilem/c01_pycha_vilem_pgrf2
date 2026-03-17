package model;

import transforms.Col;
import transforms.Point3D;
import transforms.Vec2D;
import transforms.Vec3D;

/**
 * Vrchol: 3D pozice + barva (+ případně texturovací souřadnice uv).
 */
public class Vertex {

    private final Point3D position;
    private final Col color;
    private final Vec2D uv; // může být null, pokud texturu nepoužíváme

    /** Vrchol bez barvy – použije se výchozí bílá. */
    public Vertex(double x, double y, double z) {
        this.position = new Point3D(x, y, z);
        this.color = new Col(0xffffff);
        this.uv = null;
    }

    /** Vrchol s barvou, bez textury. */
    public Vertex(double x, double y, double z, Col color) {
        this.position = new Point3D(x, y, z);
        this.color = color;
        this.uv = null;
    }

    /** Vrchol s barvou a texturovacími souřadnicemi uv. */
    public Vertex(double x, double y, double z, Col color, Vec2D uv) {
        this.position = new Point3D(x, y, z);
        this.color = color;
        this.uv = uv;
    }

    public Vertex(Point3D position, Col color) {
        this(position, color, null);
    }

    public Vertex(Point3D position, Col color, Vec2D uv, Vec3D vec) {
        this.position = position;
        this.color = color;
        this.uv = uv;
        this.vec = vec;
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
