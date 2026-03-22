package shader;

import model.Vertex;
import transforms.Col;
import transforms.Point3D;
import transforms.Vec3D;

// ShaderLit = obal kolem "zakladniho" shaderu (base).
// base muze byt: interpolace barev vrcholu, konstanta, nebo textura - vschno to jsou jen "materialove barvy".
// Pak na kazdy pixel (s world pozici a normalou) dopocitam Phong:
//   ambient  = ka * material * barva_svetla
//   diffuse  = kd * max(0, n·l) * material * barva_svetla
//   specular = ks * (r·v)^shininess * barva_svetla   (odraz svetla od hladkeho povrchu)
// Vektor l = smer ke svetlu, v = smer ke kamefe, r = odraz l od normaly.
// Kdyz lightingEnabled = false, vracim jen vystup base (napr. cisty nahled textury).
public class ShaderLit implements Shader {

    private Shader base;

    private Point3D lightPositionWorld = new Point3D(0, 0, 0.5);
    private Vec3D cameraPositionWorld = new Vec3D(0, 0, 3);
    private Col lightColor = new Col(1.0, 1.0, 1.0);
    private double lightIntensity = 1.85;
    private double ka = 0.22;
    private double kd = 1.05;
    private double ks = 0.95;
    private double shininess = 28.0;
    private boolean lightingEnabled = true;

    public ShaderLit(Shader base) {
        this.base = base != null ? base : new ShaderInterpolated();
    }

    public Shader getBase() {
        return base;
    }

    public void setBase(Shader base) {
        if (base != null) {
            this.base = base;
        }
    }

    public void setLightPositionWorld(Point3D lightPositionWorld) {
        if (lightPositionWorld != null) {
            this.lightPositionWorld = lightPositionWorld;
        }
    }

    public void setCameraPositionWorld(Vec3D cameraPositionWorld) {
        if (cameraPositionWorld != null) {
            this.cameraPositionWorld = cameraPositionWorld;
        }
    }

    public void setLightColor(Col lightColor) {
        if (lightColor != null) {
            this.lightColor = lightColor;
        }
    }

    public void setLightIntensity(double lightIntensity) {
        if (lightIntensity > 0) {
            this.lightIntensity = lightIntensity;
        }
    }

    public void setLightingEnabled(boolean lightingEnabled) {
        this.lightingEnabled = lightingEnabled;
    }

    public boolean isLightingEnabled() {
        return lightingEnabled;
    }

    @Override
    public Col getColor(Vertex pixel) {
        Col baseColor = base.getColor(pixel);
        if (baseColor == null) {
            baseColor = new Col(0xffffff);
        }
        if (!lightingEnabled) {
            return baseColor;
        }
        Point3D pos = pixel.getWorldPosition();
        Vec3D nIn = pixel.getNormal();
        // bez normaly / pozice neumim phong - necham jen barvu z base (treba osy)
        if (pos == null || nIn == null) {
            return baseColor;
        }

        Vec3D n = nIn.normalized().orElse(new Vec3D(0, 0, 1));
        Vec3D lightVec = new Vec3D(
                lightPositionWorld.getX() - pos.getX(),
                lightPositionWorld.getY() - pos.getY(),
                lightPositionWorld.getZ() - pos.getZ()
        );
        Vec3D l = lightVec.normalized().orElse(new Vec3D(0, 0, 1));

        Vec3D viewVec = new Vec3D(
                cameraPositionWorld.getX() - pos.getX(),
                cameraPositionWorld.getY() - pos.getY(),
                cameraPositionWorld.getZ() - pos.getZ()
        );
        Vec3D v = viewVec.normalized().orElse(new Vec3D(0, 0, 1));

        double ndotl = Math.max(0.0, n.dot(l));
        Vec3D r = n.mul(2.0 * n.dot(l)).sub(l).normalized().orElse(new Vec3D(0, 0, 1));
        double rdotv = Math.max(0.0, r.dot(v));
        double specPower = Math.pow(rdotv, shininess);

        // ambient + diffuse + spekularni odraz
        Col ambient = baseColor.mul(lightColor).mul(ka * lightIntensity);
        Col diffuse = baseColor.mul(lightColor).mul(kd * ndotl * lightIntensity);
        Col specular = lightColor.mul(ks * specPower * lightIntensity);
        return ambient.add(diffuse).add(specular).saturate();
    }
}
