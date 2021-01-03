/*
 * FonGuard
 * Copyright (C) 2021  Guillaume TRUCHOT <guillaume.truchot@outlook.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.fonguard;

import android.content.Context;
import android.content.SharedPreferences;

import com.fonguard.guardservice.settings.Settings;
import com.fonguard.guardservice.settings.actions.Actions;
import com.fonguard.guardservice.settings.actions.AwsS3Action;
import com.fonguard.guardservice.settings.actions.HttpAction;
import com.fonguard.guardservice.settings.actions.PhoneCallAction;
import com.fonguard.guardservice.settings.actions.PhoneMmsAction;
import com.fonguard.guardservice.settings.actions.PhoneSmsAction;
import com.fonguard.guardservice.settings.rules.Rule;
import com.fonguard.guardservice.settings.triggers.MotionTrigger;
import com.fonguard.guardservice.settings.triggers.Triggers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

public final class Preferences {
    private static final String NAME_PREFERENCES_MAIN = "FonGuard.Main";
    private static final String KEY_SETTINGS_JSON = "settingsJson";

    private static Preferences sInstance;

    private final SharedPreferences mPref;
    private Settings mSettings;


    public static Preferences getInstance(Context context) {
        if (sInstance == null) {
            synchronized (Preferences.class) {
                if (sInstance == null) {
                    sInstance = new Preferences(context);
                }
            }
        }

        return sInstance;
    }

    private Preferences(Context context) {
        mPref = context.getSharedPreferences(NAME_PREFERENCES_MAIN, Context.MODE_PRIVATE);
        loadSettings();
    }


    private void initSettings() {
        mSettings = new Settings();
        mSettings.Triggers = new Triggers();
        mSettings.Triggers.Motion = new MotionTrigger();
        mSettings.Triggers.Motion.CameraId = "";
        mSettings.Triggers.Motion.PixelValueDiffThreshold = 10;
        mSettings.Triggers.Motion.PixelNumberDiffThreshold = 10;
        mSettings.Actions = new Actions();
        mSettings.Actions.Http = new ArrayList<>();
        mSettings.Actions.AwsS3 = new ArrayList<>();
        mSettings.Actions.PhoneMms = new ArrayList<>();
        mSettings.Actions.PhoneSms = new ArrayList<>();
        mSettings.Actions.PhoneCall = new ArrayList<>();
        mSettings.Rules = new ArrayList<>();
    }

    private void loadSettings() {
        String settingsJson = mPref.getString(KEY_SETTINGS_JSON, null);

        if (settingsJson == null) {
            initSettings();
            saveSettings();
        } else {
            Gson gson = new GsonBuilder().create();
            mSettings = gson.fromJson(settingsJson, Settings.class);
        }
    }

    private void saveSettings() {
        SharedPreferences.Editor prefEditor = mPref.edit();
        Gson gson = new GsonBuilder().create();

        prefEditor.putString(KEY_SETTINGS_JSON, gson.toJson(mSettings));
        prefEditor.apply();
    }


    public String getSelectedCameraId() {
        return mSettings.Triggers.Motion.CameraId;
    }

    public int getPixelValueDiffThreshold() {
        return mSettings.Triggers.Motion.PixelValueDiffThreshold;
    }

    public int getPixelNumberDiffThreshold() {
        return mSettings.Triggers.Motion.PixelNumberDiffThreshold;
    }

    public List<HttpAction> getHttpActions() {
        return mSettings.Actions.Http;
    }

    public List<AwsS3Action> getAwsS3Actions() {
        return mSettings.Actions.AwsS3;
    }

    public List<PhoneMmsAction> getPhoneMmsActions() {
        return mSettings.Actions.PhoneMms;
    }

    public List<PhoneSmsAction> getPhoneSmsActions() {
        return mSettings.Actions.PhoneSms;
    }

    public List<PhoneCallAction> getPhoneCallActions() {
        return mSettings.Actions.PhoneCall;
    }

    public List<Rule> getRules() {
        return mSettings.Rules;
    }


    public void setSelectedCameraId(String selectedCameraId) {
        mSettings.Triggers.Motion.CameraId = selectedCameraId;
        saveSettings();
    }

    public void setPixelValueDiffThreshold(int pixelValueDiffThreshold) {
        mSettings.Triggers.Motion.PixelValueDiffThreshold = pixelValueDiffThreshold;
        saveSettings();
    }

    public void setPixelNumberDiffThreshold(int pixelNumberDiffThreshold) {
        mSettings.Triggers.Motion.PixelNumberDiffThreshold = pixelNumberDiffThreshold;
        saveSettings();
    }


    public Settings getSettings() {
        return mSettings;
    }

    public void setSettings(Settings settings) {
        mSettings = settings;
        saveSettings();
    }
}
