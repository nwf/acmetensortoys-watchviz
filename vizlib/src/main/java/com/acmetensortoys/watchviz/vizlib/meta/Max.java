package com.acmetensortoys.watchviz.vizlib.meta;

import com.acmetensortoys.watchviz.vizlib.Meta;

public class Max extends Meta {
    public float window = Float.NEGATIVE_INFINITY;
    public int ix = 0;
    final public float[] list;

    public Max(int shift) {
        if (shift < 0 || shift > 16) {
            throw new RuntimeException("Improper shift: " + shift);
        }
        list = new float[1<<shift];
    }

    @Override
    public float get() {
        return window;
    }

    @Override
    public void update(float last) {
        if (last > window) {
            // If new thing is bigger, set both...
            window = list[ix] = last;
        } else if (list[ix] < window) {
            // if smaller but current ix is not witness...
            list[ix] = last;
        } else {
            // Whoop, smaller and current thing maybe is the witness...
            list[ix] = last;
            window = last;
            for (float f : list) { window = Math.max(window, f); }
        }

        ix = (ix + 1) % list.length;
    }
}
