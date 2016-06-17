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

package com.acmetensortoys.watchviz;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.acmetensortoys.watchviz.vizlib.AudioCanvas;

public class MainActivity extends WearableActivity
{
    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mOuterContainer;
    private LinearLayout mInnerContainer;
    private TextView mTextView, mClockView, mDebugView;
    private SurfaceView mACSurfaceView;
    private AudioCanvas mAudioCanvas;

    private void createSurface() {
        Log.d("createSurface", "top");
        mACSurfaceView = new SurfaceView(this);
        mAudioCanvas = new AudioCanvas(mDebugView, mACSurfaceView);
        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioCanvas.prevCyclerCB();
            }
        });
        mACSurfaceView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) { mAudioCanvas.onClick(); }
        });
        mACSurfaceView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mAudioCanvas.nextCyclerCB();
                return true;
            }
        });
        mInnerContainer.addView(mACSurfaceView, -1,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void removeSurface() {
        Log.d("removeSurface", "removing view");
        mInnerContainer.removeView(mACSurfaceView);
        Log.d("removeSurface", "done");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mOuterContainer = (BoxInsetLayout) findViewById(R.id.top_container);
        mInnerContainer = (LinearLayout) findViewById(R.id.linear_container);
        mTextView = (TextView) findViewById(R.id.text);
        mDebugView = (TextView) findViewById(R.id.dbg);
        mClockView = (TextView) findViewById(R.id.clock);

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
            createSurface();
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
                createSurface();
            } else {
                mDebugView.setText("No audio");
            }
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        final int black = getResources().getColor(android.R.color.black,null);
        mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));

        if (isAmbient()) {
            final int white = getResources().getColor(android.R.color.white,null);
            mOuterContainer.setBackgroundColor(black);
            mTextView.setTextColor(white);
            mClockView.setTextColor(white);
            mDebugView.setTextColor(white);
        } else {
            mOuterContainer.setBackground(null);
            mTextView.setTextColor(black);
            mClockView.setTextColor(black);
            mDebugView.setTextColor(black);
        }
    }

    @Override
    public void onPause() {
        Log.d("onPause", "top");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d("onStop", "top");
        removeSurface();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d("onDestroy", "top");
        super.onDestroy();
    }
}
