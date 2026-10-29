package dev.shaman.bicolor;

import org.jspecify.annotations.Nullable;

public interface ColoredBox {
    @Nullable ColorPair bicolor$getColors();
    void bicolor$setColors(@Nullable ColorPair colors);
}
