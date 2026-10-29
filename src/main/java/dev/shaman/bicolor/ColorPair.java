package dev.shaman.bicolor;

import java.util.Objects;
import java.util.Optional;

public record ColorPair(ShellColor lid, ShellColor base) {
    public ColorPair { Objects.requireNonNull(lid); Objects.requireNonNull(base); }
    public boolean isUniform() { return lid == base; }
    public static Optional<ColorPair> parse(String lid, String base) {
        return ShellColor.parse(lid).flatMap(l -> ShellColor.parse(base).map(b -> new ColorPair(l, b)));
    }
}
