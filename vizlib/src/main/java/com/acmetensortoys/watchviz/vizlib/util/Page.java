package com.acmetensortoys.watchviz.vizlib.util;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.acmetensortoys.watchviz.vizlib.AudioProvider;

public abstract class Page {
    public interface VizLibUI {
        Activity getActivity();
        AudioProvider getAudioProvider();
    }

    public abstract static class InstantiatedPage {
        abstract public boolean ownsview(View v);
        abstract public void deinstantiate(VizLibUI m, ViewGroup g);
    }

    /* Some Pages just contain View objects directly; go ahead and define those here */
    public static class ViewInstantiatedPage extends InstantiatedPage {
        private final View v;

        public ViewInstantiatedPage(View v) {
            this.v = v;
        }

        @Override
        public boolean ownsview(View v2) {
            return v == v2;
        }

        @Override
        public void deinstantiate(Page.VizLibUI m, ViewGroup g) {
            g.removeView(v);
        }
    }

    public String title;

    abstract public InstantiatedPage instantiate(VizLibUI m, ViewGroup g, int r, int c);
}
