package com.acmetensortoys.watchviz.vizlib.util;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

public class ProgrammablePreferenceFragment extends PreferenceFragment {
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
        Log.d("PPF", "sp=" + getPreferenceManager().getSharedPreferences().toString());
        addPreferencesFromResource(getArguments().getInt(ARG_RES_IX));
    }
}
