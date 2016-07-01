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

import android.content.Context;
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

    /* All returned Objects are actually Pages so we know what to do with them later */
    private abstract static class InstantiatedPage {
        abstract public boolean ownsview(View v);
        abstract public void deinstantiate(MainGridViewAdapter m, ViewGroup g);
    };
    private static class ViewInstantiatedPage extends InstantiatedPage {
        public View v;
        public ViewInstantiatedPage(View v) { this.v = v; }
        @Override
        public boolean ownsview(View v2) { return v.equals(v2); }
        @Override
        public void deinstantiate(MainGridViewAdapter m, ViewGroup g) { g.removeView(v); }
    }
    private abstract static class Page {
        public String title;
        abstract public InstantiatedPage instantiate(MainGridViewAdapter m, ViewGroup g, int r, int c);
    }
    private static class RendererPage extends Page {
        private Class<? extends Rendering> re;
        public RendererPage(Class<? extends Rendering> r) {
            this.re = r;
            this.title = r.getSimpleName();
        }
        public InstantiatedPage instantiate(MainGridViewAdapter mgva, ViewGroup g, int r, int c) {
            SurfaceView sv = new SurfaceView(mgva.mCtx);
            AudioCanvas ac = new AudioCanvas(r+"x"+c, sv, mgva.mAp);
            try {
                ac.setRendering(re.getConstructor().newInstance());
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

    private Page gridPage = new RendererPage(Grid.class);
    private Page wholePage = new RendererPage(WholeMax.class);
    private final Page[][] pages = {
            {gridPage, gridPage},
            {wholePage }
    };

    private final Context mCtx;
    private final AudioProvider mAp;
    private final TextView mDv;

    public MainGridViewAdapter(Context c, AudioProvider ap, TextView dv) {
        mCtx = c;
        mAp = ap;
        mDv = dv;
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

    @Override
    public void finishUpdate(ViewGroup container) {
        // super.finishUpdate(container);
        Log.d("MGVA", "finishUpdate bottom");
    }

    @Override
    public void onPageScrolled(int i, int i1, float v, float v1, int i2, int i3) {
        // Log.d("MGVA", "scrolled");
    }

    @Override
    public void onPageSelected(int row, int col) {
        // Log.d("MGVA", "selected");
        final String name = pages[row][col].title;
        mDv.post(new Runnable() {
            @Override
            public void run() {
                mDv.setText(name.substring(0, Math.min(10, name.length())));
            }
        });
    }

    @Override
    public void onPageScrollStateChanged(int i) {
        ;
    }
}
