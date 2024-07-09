package com.hyf.jad;

/**
 * @author baB_hyf
 * @date 2022/01/23
 */
public class Pair<X, Y> {
    private final X x;
    private final Y y;

    public Pair(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    public static <A, B> Pair<A, B> make(A a, B b) {
        return new Pair<A, B>(a, b);
    }

    public X getX() {
        return x;
    }

    public Y getY() {
        return y;
    }
}