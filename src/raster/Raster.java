package raster;

import transforms.Col;

import java.util.Optional;


public interface Raster <T>   //T promena co bude prezetntovat vsechny ty promene
{
    void setValue(int x, int y, T value);
    Optional<T> getValue(int x, int y);
    int getWidth();
    int getHeight();
    void clear();
}
