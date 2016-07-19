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

/* This file is responsible for navigation of the UI; the Pages are constructed
 * elsewhere, this thing just manages what's on the screen.  Note that we expect
 * views that need to know whether they're on screen to find out via some other
 * callback; we don't do anything here.
 */

package com.acmetensortoys.watchviz;

import android.app.Activity;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.acmetensortoys.watchviz.vizlib.AudioProvider;
import com.acmetensortoys.watchviz.vizlib.util.Page;

public class MainGridViewAdapter
        extends GridPagerAdapter
        implements GridViewPager.OnPageChangeListener, Page.VizLibUI {

    private final Activity mAct;    // Also a Context
    private final AudioProvider mAp;
    private final TextView mDv;
    private final Page[][] pages;

    public MainGridViewAdapter(Activity a, AudioProvider ap, TextView dv, Page[][] pages) {
        mAct = a;
        mAp = ap;
        mDv = dv;
        this.pages = pages;
    }

    /* All returned Objects are actually Pages so we know what to do with them later */

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
        return ((Page.InstantiatedPage)o).ownsview(view);
    }

    @Override
    public void destroyItem(ViewGroup viewGroup, int row, int col, Object o) {
        Log.d("MGVA", "destroy:" + row + "x" + col);
        ((Page.InstantiatedPage)o).deinstantiate(this, viewGroup);
    }

    @Override
    public void startUpdate(ViewGroup container) {
        Log.d("MGVA", "startUpdate");
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        Log.d("MGVA", "finishUpdate");
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

    }

    @Override
    public Activity getActivity() { return mAct; }

    @Override
    public AudioProvider getAudioProvider() { return mAp; }
}
