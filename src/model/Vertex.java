package model;

import transforms.Col;
import transforms.Point3D;

import java.awt.*;

public class Vertex {

    private final Point3D position;
    private Col color;

    public Vertex(double x, double y, double z) {
        this.position = new Point3D(x, y, z);
        this.color = new Col(0xffffff);
    }

    public Vertex(double x, double y, double z, Col color) {
        this.position = new Point3D(x, y, z);
        this.color = color;
    }

    public Point3D getPosition() {
        return position;
    }
    public Col getColor() {
        return color;
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
