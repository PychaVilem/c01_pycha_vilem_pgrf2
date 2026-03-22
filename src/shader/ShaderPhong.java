package shader;

// Zachovane jmeno tridy: drive to byl samostatny phong, ted je to jen
// ShaderLit(new ShaderInterpolated()) aby stare instanceof ShaderPhong nekde fungovaly.
public class ShaderPhong extends ShaderLit {

    public ShaderPhong() {
        super(new ShaderInterpolated());
    }
}
