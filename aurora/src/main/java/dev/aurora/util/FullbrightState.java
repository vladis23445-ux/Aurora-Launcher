package dev.aurora.util;

/** Состояние Fullbright: подмена гаммы и отключение затемнения. */
public final class FullbrightState {

    public static boolean active;
    public static double gamma = 15.0;

    private FullbrightState() {
    }
}
