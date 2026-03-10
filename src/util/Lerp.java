package util;

import transforms.Col;

// Jednoduché pomocné metody pro lineární interpolaci (LERP).
// Zatím řešíme jen double a barvu Col, případné generické verze se dají dodělat později.
public class Lerp {

    // LERP pro čísla: t=0 -> a, t=1 -> b
    public static double lerp(double a, double b, double t) {
        return (1 - t) * a + t * b;
    }

    // LERP pro barvu: mix dvou barev podle t
    public static Col lerp(Col a, Col b, double t) {
        return a.mul(1 - t).add(b.mul(t)).saturate();
    }
}
