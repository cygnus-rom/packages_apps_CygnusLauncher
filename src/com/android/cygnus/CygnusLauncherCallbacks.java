/*
 * Copyright (C) 2020 Cygnus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.cygnus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.core.graphics.ColorUtils;

import com.android.launcher3.AppInfo;
import com.android.launcher3.LauncherCallbacks;
import com.android.launcher3.R;
import com.android.launcher3.settings.SettingsActivity;
import com.android.launcher3.uioverrides.WallpaperColorInfo;
import com.android.launcher3.uioverrides.WallpaperColorInfo.OnChangeListener;
import com.android.launcher3.util.Themes;
import com.android.launcher3.Utilities;

import com.cygnus.launcher.qsb.QsbAnimationController;

import com.google.android.libraries.gsa.launcherclient.ClientOptions;
import com.google.android.libraries.gsa.launcherclient.ClientService;
import com.google.android.libraries.gsa.launcherclient.LauncherClient;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class CygnusLauncherCallbacks implements LauncherCallbacks,
        SharedPreferences.OnSharedPreferenceChangeListener, OnChangeListener {
    public static final String SEARCH_PACKAGE = "com.google.android.googlequicksearchbox";

    private final CygnusLauncher mLauncher;

    private OverlayCallbackImpl mOverlayCallbacks;
    private LauncherClient mLauncherClient;
    private QsbAnimationController mQsbController;
    private SharedPreferences mPrefs;

    private boolean mStarted;
    private boolean mResumed;
    private boolean mAlreadyOnHome;

    private final Bundle mUiInformation = new Bundle();

    public CygnusLauncherCallbacks(CygnusLauncher launcher) {
        mLauncher = launcher;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = Utilities.getPrefs(mLauncher);
        mOverlayCallbacks = new OverlayCallbackImpl(mLauncher);
        mLauncherClient = new LauncherClient(mLauncher, mOverlayCallbacks, getClientOptions(mPrefs));
        mQsbController = new QsbAnimationController(mLauncher);
        mOverlayCallbacks.setClient(mLauncherClient);
        mUiInformation.putInt("system_ui_visibility", mLauncher.getWindow().getDecorView().getSystemUiVisibility());
        WallpaperColorInfo instance = WallpaperColorInfo.getInstance(mLauncher);
        instance.addOnChangeListener(this);
        onExtractedColorsChanged(instance);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        mResumed = true;
        if (mStarted) {
            mAlreadyOnHome = true;
        }

        mLauncherClient.onResume();
    }

    @Override
    public void onStart() {
        mStarted = true;
        mLauncherClient.onStart();
    }

    @Override
    public void onStop() {
        mStarted = false;
        if (!mResumed) {
            mAlreadyOnHome = false;
        }

        mLauncherClient.onStop();
    }

    @Override
    public void onPause() {
        mResumed = false;
        mLauncherClient.onPause();
    }

    @Override
    public void onDestroy() {
        mLauncherClient.onDestroy();

        Utilities.getPrefs(mLauncher).unregisterOnSharedPreferenceChangeListener(this);
        WallpaperColorInfo.getInstance(mLauncher).removeOnChangeListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

    }

    @Override
    public void onAttachedToWindow() {
        mLauncherClient.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        mLauncherClient.onDetachedFromWindow();
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter w, String[] args) {

    }

    @Override
    public void onHomeIntent(boolean internalStateHandled) {
        mLauncherClient.hideOverlay(mAlreadyOnHome);
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public void onTrimMemory(int level) {

    }

    @Override
    public void onLauncherProviderChange() {

    }

    @Override
    public boolean startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData) {
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingsActivity.KEY_MINUS_ONE.equals(key)) {
            mLauncherClient.setClientOptions(getClientOptions(sharedPreferences));
    }

    @Override
    public void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {
        int alpha = mLauncher.getResources().getInteger(R.integer.extracted_color_gradient_alpha);
        mUiInformation.putInt("background_color_hint", primaryColor(wallpaperColorInfo, mLauncher, alpha));
        mUiInformation.putInt("background_secondary_color_hint", secondaryColor(wallpaperColorInfo, mLauncher, alpha));
        mUiInformation.putBoolean("is_background_dark", Themes.getAttrBoolean(mLauncher, R.attr.isMainColorDark));
        mLauncherClient.redraw(mUiInformation);
    }

    @Override
    public LauncherClient getClient() {
        return mLauncherClient;
    }

    @Override
    public QsbAnimationController getQsbController() {
        return mQsbController;
        }
    }

    private LauncherClient.ClientOptions getClientOptions(SharedPreferences prefs) {
        boolean hasPackage = LineageUtils.hasPackageInstalled(mLauncher, SEARCH_PACKAGE);
        boolean isEnabled = prefs.getBoolean(SettingsActivity.KEY_MINUS_ONE, true);
        return new LauncherClient.ClientOptions(hasPackage && isEnabled,
                true, /* enableHotword */
                true /* enablePrewarming */
        );
    }
}
