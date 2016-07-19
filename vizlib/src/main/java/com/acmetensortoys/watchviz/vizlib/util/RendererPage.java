package com.acmetensortoys.watchviz.vizlib.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.acmetensortoys.watchviz.vizlib.AudioCanvas;
import com.acmetensortoys.watchviz.vizlib.Rendering;

public class RendererPage extends Page {
    public static final String RENDERER_GLOBAL_SHARED_PREF_NAME = "render_globals";

    private final Class<? extends Rendering> re;
    private final String spn;

    public RendererPage(Class<? extends Rendering> r, String spn) {
        this.re = r;
        this.spn = spn;
        this.title = r.getSimpleName();
    }

    @Override
    public InstantiatedPage instantiate(VizLibUI m, ViewGroup g, int r, int c) {
        final Activity a = m.getActivity();
        final SurfaceView sv = new SurfaceView(a);
        final AudioCanvas ac = new AudioCanvas(r + "x" + c, sv, m.getAudioProvider());
        final Rendering rend;
        try {
            rend = re.getConstructor(SharedPreferences.class, SharedPreferences.class)
                    .newInstance(
                            a.getSharedPreferences(spn, Context.MODE_PRIVATE),
                            a.getSharedPreferences(RENDERER_GLOBAL_SHARED_PREF_NAME, Context.MODE_PRIVATE));
            ac.setRendering(rend);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        g.addView(sv, -1,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        return new ViewInstantiatedPage(sv);
    }
}
