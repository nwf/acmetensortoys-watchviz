/*
    AcmeTensorToys WatchViz: a sound visualization application for Android Wear
    Copyright (C) 2016 Nathaniel Wesley Filardo

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/* This file is responsible for navigation of the UI, through different renderers
   and (eventually), their settings, as well as global preferences and all that.
 */


package com.acmetensortoys.watchviz;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.acmetensortoys.watchviz.vizlib.AudioCanvas;
import com.acmetensortoys.watchviz.vizlib.AudioProvider;
import com.acmetensortoys.watchviz.vizlib.Rendering;
import com.acmetensortoys.watchviz.vizlib.rendering.Grid;
import com.acmetensortoys.watchviz.vizlib.rendering.WholeMax;

public class MainGridViewAdapter
        extends GridPagerAdapter
        implements GridViewPager.OnPageChangeListener {

    private final Activity mAct;    // Also a Context
    private final AudioProvider mAp;
    private final TextView mDv;
    private final Page[][] pages;

    public MainGridViewAdapter(Activity a, AudioProvider ap, TextView dv) {
        mAct = a;
        mAp = ap;
        mDv = dv;

        Page gridPage = new RendererPage(Grid.class,
                mAct.getSharedPreferences("grid",Context.MODE_PRIVATE));
        Page wholePage = new RendererPage(WholeMax.class,
                mAct.getSharedPreferences("whole",Context.MODE_PRIVATE));

        pages = new Page[][]{
            {gridPage, new FragmentPage(ProgrammablePreferenceFragment
                            .newInstance(R.xml.pref_grid, "grid"))},
            {wholePage }
        };
    }

    /* All returned Objects are actually Pages so we know what to do with them later */
    private abstract static class InstantiatedPage {
        abstract public boolean ownsview(View v);
        abstract public void deinstantiate(MainGridViewAdapter m, ViewGroup g);
    };
    private abstract static class Page {
        public String title;
        abstract public InstantiatedPage instantiate(MainGridViewAdapter m, ViewGroup g, int r, int c);
    }

    /* Some Pages just contain View objects directly */
    private static class ViewInstantiatedPage extends InstantiatedPage {
        private final View v;
        public ViewInstantiatedPage(View v) { this.v = v; }
        @Override
        public boolean ownsview(View v2) { return v == v2; }
        @Override
        public void deinstantiate(MainGridViewAdapter m, ViewGroup g) { g.removeView(v); }
    }
    private static class RendererPage extends Page {
        private final Class<? extends Rendering> re;
        private final SharedPreferences lsp;
        public RendererPage(Class<? extends Rendering> r, SharedPreferences lsp) {
            this.re = r;
            this.lsp = lsp;
            this.title = r.getSimpleName();
        }
        public InstantiatedPage instantiate(MainGridViewAdapter mgva, ViewGroup g, int r, int c) {
            final SurfaceView sv = new SurfaceView(mgva.mAct);
            final AudioCanvas ac = new AudioCanvas(r+"x"+c, sv, mgva.mAp);
            final Rendering rend;
            try {
                rend = re.getConstructor(SharedPreferences.class, SharedPreferences.class)
                        .newInstance(lsp,
                                mgva.mAct.getPreferences(Context.MODE_PRIVATE));
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

    /* And some manage Fragments; note that these require assistance from finishUpdate below */
    private FragmentTransaction mFT = null;
    private static class FragmentInstantedPage extends InstantiatedPage {
        private final Fragment f;

        public FragmentInstantedPage(Fragment f) { this.f = f; }

        @Override
        public boolean ownsview(View v) {
            return v == f.getView();
        }

        @Override
        public void deinstantiate(MainGridViewAdapter m, ViewGroup g) {
            m.startFragmentTransaction().remove(f);
        }
    }
    private static class FragmentPage extends Page {
        private final Fragment f;
        public FragmentPage(Fragment f) { this.f = f; }

        @Override
        public InstantiatedPage instantiate(MainGridViewAdapter m, ViewGroup g, int r, int c) {
            m.startFragmentTransaction().add(g.getId(),f);
            return new FragmentInstantedPage(f);
        }
    }

    public static class ProgrammablePreferenceFragment extends PreferenceFragment {
        private final static String ARG_RES_IX = "res";
        private final static String ARG_PREF_IX = "pref";

        public static ProgrammablePreferenceFragment newInstance(int res, String pref) {
            ProgrammablePreferenceFragment f = new ProgrammablePreferenceFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_RES_IX,res);
            args.putString(ARG_PREF_IX,pref);
            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(Bundle sis) {
            super.onCreate(sis);
            getPreferenceManager().setSharedPreferencesName(getArguments().getString(ARG_PREF_IX));
            Log.d("PPF", "sp="+getPreferenceManager().getSharedPreferences().toString());
            addPreferencesFromResource(getArguments().getInt(ARG_RES_IX));
        }
    }

    @Override
    public int getRowCount() {
        return pages.length;
    }

    @Override
    public int getCurrentColumnForRow(int row, int currentColumn) {
        return 0;
    }

    @Override
    public int getColumnCount(int i) {
        return pages[i].length;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int row, int col) {
        Log.d("MGVA", "instantiate:" + row + "x" + col);
        return pages[row][col].instantiate(this, container, row, col);
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return ((InstantiatedPage)o).ownsview(view);
    }

    @Override
    public void destroyItem(ViewGroup viewGroup, int row, int col, Object o) {
        Log.d("MGVA", "destroy:" + row + "x" + col);
        ((InstantiatedPage)o).deinstantiate(this, viewGroup);
    }

    @Override
    public void startUpdate(ViewGroup container) {
        // Log.d("MGVA", "startUpdate top");
        super.startUpdate(container);
    }

    FragmentTransaction startFragmentTransaction() {
        if(mFT != null) { return mFT; }
        return (mFT = mAct.getFragmentManager().beginTransaction());
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (mAct.getFragmentManager().isDestroyed()) {
            mFT = null;
        } else if (mFT != null) {
            mFT.commitAllowingStateLoss();
            mFT = null;
            mAct.getFragmentManager().executePendingTransactions();
            container.postInvalidate(); // TODO: does this help and if so why?
        }

        super.finishUpdate(container);
        //Log.d("MGVA", "finishUpdate bottom");
    }

    @Override
    public void onPageScrolled(int i, int i1, float v, float v1, int i2, int i3) {
        // Log.d("MGVA", "scrolled");
    }

    @Override
    public void onPageSelected(int row, int col) {
        // Log.d("MGVA", "selected");
        final String name = pages[row][col].title;
        if (name != null) {
            mDv.post(new Runnable() {
                @Override
                public void run() {
                    mDv.setText(name.substring(0, Math.min(10, name.length())));
                }
            });
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {
        ;
    }
}
