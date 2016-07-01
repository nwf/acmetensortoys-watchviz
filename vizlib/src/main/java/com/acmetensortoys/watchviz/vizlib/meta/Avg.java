package com.acmetensortoys.watchviz.vizlib.meta;

import com.acmetensortoys.watchviz.vizlib.Meta;

public class Avg extends Meta {
    public float window = 0f;
    public int ix = 0;
    public float[] list;

    public Avg(int shift) {
        list = new float[1<<shift];
    }

    @Override
    public float get() {
        return window / list.length;
    }

    @Override
    public void update(float last) {
        window -= list[ix];
        list[ix] = last;

        if(ix == list.length-1) {
            window = 0;
            for(float v : list) { window += v; }
            ix = 0;
        } else {
            window += list[ix];
            ix += 1;
        }
    }
}
