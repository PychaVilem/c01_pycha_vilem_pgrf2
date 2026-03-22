package raster;

import transforms.Col;

import java.util.Optional;


// 2D pole hodnot (barva nebo hloubka) - rozhrani pro raster a depth buffer
public interface Raster <T>
{
    void setValue(int x, int y, T value);
    Optional<T> getValue(int x, int y);
    int getWidth();
    int getHeight();
    void clear();
}
