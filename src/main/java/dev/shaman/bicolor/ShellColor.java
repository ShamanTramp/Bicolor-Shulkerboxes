package dev.shaman.bicolor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ShellColor {
    NATURAL(0x9469a5, "naturelle"), WHITE(0xf9fffe, "blanc"), ORANGE(0xf9801d, "orange"),
    MAGENTA(0xc74ebd, "magenta"), LIGHT_BLUE(0x3ab3da, "bleu clair"), YELLOW(0xfed83d, "jaune"),
    LIME(0x80c71f, "vert clair"), PINK(0xf38baa, "rose"), GRAY(0x474f52, "gris"),
    LIGHT_GRAY(0x9d9d97, "gris clair"), CYAN(0x169c9c, "cyan"), PURPLE(0x8932b8, "violet"),
    BLUE(0x3c44aa, "bleu"), BROWN(0x835432, "marron"), GREEN(0x5e7c16, "vert"),
    RED(0xb02e26, "rouge"), BLACK(0x1d1d21, "noir");

    private final int rgb;
    private final String french;
    ShellColor(int rgb, String french) { this.rgb = rgb; this.french = french; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public int rgb() { return rgb; }
    public String french() { return french; }
    public String english() { return id().replace('_', ' '); }
    public static Optional<ShellColor> parse(String id) {
        return Arrays.stream(values()).filter(c -> c.id().equals(id)).findFirst();
    }
}
