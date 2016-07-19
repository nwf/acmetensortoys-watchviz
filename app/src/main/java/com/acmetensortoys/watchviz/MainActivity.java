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

/*
  This is the glue code that provides the Activity that Android interacts with.
  Most of the actual UI work of interest is in MainGridViewAdapter.
 */

package com.acmetensortoys.watchviz;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.acmetensortoys.watchviz.vizlib.AudioProvider;
import com.acmetensortoys.watchviz.vizlib.rendering.Grid;
import com.acmetensortoys.watchviz.vizlib.rendering.WholeMax;
import com.acmetensortoys.watchviz.vizlib.util.FragmentPage;
import com.acmetensortoys.watchviz.vizlib.util.Page;
import com.acmetensortoys.watchviz.vizlib.util.RendererPage;

public class MainActivity extends WearableActivity
{
    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    private static final int TIME_UPDATE_NOT_AMBIENT_PERIOD = 2000;

    private Handler mHandler;
    private RelativeLayout mTextContainer;
    private TextView mTextView, mClockView, mDebugView;
    private AudioProvider mAudioProvider = new AudioProvider();

    private GridViewPager mGridView;

    Page[][] pages = new Page[][]
            {
                    {new RendererPage(Grid.class, "grid"),
                            FragmentPage.makeProgrammablePreferencePage(R.xml.pref_grid, "grid", "grid pref")},

                    {new RendererPage(WholeMax.class, "whole")},

                    /* {MainGridViewAdapter.makeProgrammablePreferencePage(R.xml.pref_grid,
                            MainGridViewAdapter.RENDERER_GLOBAL_SHARED_PREF_NAME,
                            "global")} */
            };

    private void createSurfaces() {
        Log.d("createSurfaces", "top");
        MainGridViewAdapter mgva = new MainGridViewAdapter(this, mAudioProvider, mDebugView, pages);
        mGridView.setOnPageChangeListener(mgva);
        mGridView.setAdapter(mgva);
        // Fake the selection callback.  Why doesn't this happen automatically? :(
        mgva.onPageSelected(mGridView.getCurrentItem().x, mGridView.getCurrentItem().y);
    }

    private void removeSurfaces() {
        Log.d("removeSurfaces", "removing grid view");
        mGridView.setAdapter(null);
        Log.d("removeSurfaces", "done");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // grab an interface to our local looper
        mHandler = new Handler();

        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mTextContainer = (RelativeLayout) findViewById(R.id.text_container);
        mTextView = (TextView) findViewById(R.id.text);
        mDebugView = (TextView) findViewById(R.id.dbg);
        mClockView = (TextView) findViewById(R.id.clock);
        mGridView = (GridViewPager) findViewById(R.id.grid);
        // mGridView.setBackgroundColor(Color.WHITE);
        // mGridView.setDrawingCacheBackgroundColor(Color.WHITE);
        // mGridView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        updateDisplay();
    }

    @Override
    public void onStart() {
        Log.d("onStart", "top");
        super.onStart();

        // Ask once at startup
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else {
            createSurfaces();
        }

        Log.d("onStart", "BLE: " + getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
        // final BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    }

    @Override
    public void onResume() {
        Log.d("onResume", "top");
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int rq, @NonNull String[] ps, @NonNull int[] rs) {
        if(rq == 1) {
            if (rs[0] == PackageManager.PERMISSION_GRANTED) {
                createSurfaces();
            } else {
                mDebugView.setText("No audio");
            }
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        mHandler.removeCallbacks(updateTimeNotAmbient);
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        updateDisplay();
        mHandler.postDelayed(updateTimeNotAmbient, TIME_UPDATE_NOT_AMBIENT_PERIOD);
    }

    private void updateDisplay() {
        final int black = getResources().getColor(android.R.color.black,null);
        final int white = getResources().getColor(android.R.color.white,null);
        updateTime();

        if (isAmbient()) {
            mTextContainer.setBackgroundColor(black);
            mTextView.setTextColor(white);
            mClockView.setTextColor(white);
            mDebugView.setTextColor(white);
        } else {
            mTextContainer.setBackgroundColor(white);
            mTextView.setTextColor(black);
            mClockView.setTextColor(black);
            mDebugView.setTextColor(black);
        }
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateTime();
    }

    private Runnable updateTimeNotAmbient = new Runnable() {
        @Override
        public void run() {
            updateTime();
            mHandler.postDelayed(this, TIME_UPDATE_NOT_AMBIENT_PERIOD);
        }
    };

    private void updateTime() {
        mClockView.post(new Runnable() {
            @Override
            public void run() {
                mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
            }
        });
    }

    @Override
    public void onPause() {
        Log.d("onPause", "top");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d("onStop", "top");
        removeSurfaces();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d("onDestroy", "top");
        super.onDestroy();
    }
}
