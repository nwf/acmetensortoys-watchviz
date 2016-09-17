package com.acmetensortoys.watchviz.vizlib.util;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.BaseAdapter;

public class ProgrammablePreferenceFragment extends PreferenceFragment
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private final static String ARG_RES_IX = "res";
    private final static String ARG_PREF_IX = "pref";

    public static ProgrammablePreferenceFragment newInstance(int res, String pref) {
        ProgrammablePreferenceFragment f = new ProgrammablePreferenceFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_RES_IX, res);
        args.putString(ARG_PREF_IX, pref);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle sis) {
        super.onCreate(sis);
        getPreferenceManager().setSharedPreferencesName(getArguments().getString(ARG_PREF_IX));
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        Log.d("PPF", "sp=" + getPreferenceManager().getSharedPreferences().toString());
        addPreferencesFromResource(getArguments().getInt(ARG_RES_IX));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("PPF", "OSPCL sp="+getPreferenceManager().getSharedPreferences().toString());
        // XXX Phenomenal
        setPreferenceScreen(null);
        addPreferencesFromResource(getArguments().getInt(ARG_RES_IX));
    }
}
