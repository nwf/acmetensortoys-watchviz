package com.acmetensortoys.watchviz.vizlib;

import android.widget.TextView;

import com.acmetensortoys.watchviz.vizlib.rendering.Grid;
import com.acmetensortoys.watchviz.vizlib.rendering.WholeMax;

import java.util.ArrayList;

public class RenderingSelector {
    private TextView mDebugView;
    private Renderer mRenderer;

    private int renderCBix;
    private ArrayList<Class<? extends Rendering>> cbs = new ArrayList<>();

    public RenderingSelector(TextView dv, Renderer r) {
        mDebugView = dv;
        mRenderer = r;
        cbs.add(Grid.class);
        cbs.add(WholeMax.class);
        renderCBix = 0;
        _setCyclerCBByIx();
    }

    private void _setCyclerCB(Class<? extends Rendering> next) {
        Rendering rendering;
        try {
            rendering = next.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        mRenderer.setRendering(rendering);
        String name = rendering.getClass().getSimpleName();
        mDebugView.setText(name.substring(0,Math.min(10,name.length())));
    }
    private void _setCyclerCBByIx() {
        _setCyclerCB(cbs.get(renderCBix));
    }
    public void nextCyclerCB() {
        synchronized(this) {
            if (renderCBix == cbs.size()-1) {
                renderCBix = 0;
            } else {
                renderCBix += 1;
            }
            _setCyclerCBByIx();
        }
    }
    public void prevCyclerCB() {
        synchronized(this) {
            if (renderCBix == 0) {
                renderCBix = cbs.size()-1;
            } else {
                renderCBix -= 1;
            }
            _setCyclerCBByIx();
        }
    }
    public void setCBIx(int ix) {
        if ((ix < 0) || (ix >= cbs.size())) {
            throw new ArrayIndexOutOfBoundsException(ix);
        }
        synchronized(this) {
            renderCBix = ix;
            _setCyclerCBByIx();
        }
    }
}
