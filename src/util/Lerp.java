package util;

import transforms.Col;

// linearni interpolace - pri rasterizaci trojuhelniku mezi vrcholy
public class Lerp {

    // t=0 -> a, t=1 -> b
    public static double lerp(double a, double b, double t) {
        return (1 - t) * a + t * b;
    }

    public static Col lerp(Col a, Col b, double t) {
        return a.mul(1 - t).add(b.mul(t)).saturate();
    }
}
