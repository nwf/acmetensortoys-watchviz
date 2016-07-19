package com.acmetensortoys.watchviz.vizlib.util;

import android.app.Fragment;
import android.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;

public class FragmentPage extends Page {
    public static class FragmentInstantedPage extends InstantiatedPage {
        private final Fragment f;

        public FragmentInstantedPage(Fragment f) {
            this.f = f;
        }

        @Override
        public boolean ownsview(View v) {
            return v == f.getView();
        }

        @Override
        public void deinstantiate(VizLibUI m, ViewGroup g) {
            FragmentManager fm = m.getActivity().getFragmentManager();
            fm.beginTransaction().remove(f).commitAllowingStateLoss();
            fm.executePendingTransactions();
        }
    }

    private final Fragment f;

    public FragmentPage(Fragment f) {
        this.f = f;
    }

    public FragmentPage(Fragment f, String t) {
        this(f);
        this.title = t;
    }

    @Override
    public InstantiatedPage instantiate(VizLibUI m, ViewGroup g, int r, int c) {
        FragmentManager fm = m.getActivity().getFragmentManager();
        fm.beginTransaction().add(g.getId(), f).commitAllowingStateLoss();
        fm.executePendingTransactions();
        return new FragmentInstantedPage(f);
    }

    public static FragmentPage makeProgrammablePreferencePage(int x, String pn, String t) {
        return new FragmentPage(ProgrammablePreferenceFragment.newInstance(x, pn), t);
    }
}
