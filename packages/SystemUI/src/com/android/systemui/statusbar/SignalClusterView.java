/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.statusbar;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.statusbar.policy.NetworkController;

import com.android.systemui.R;

// Intimately tied to the design of res/layout/signal_cluster_view.xml
public class SignalClusterView
        extends LinearLayout
        implements NetworkController.SignalCluster {

    static final boolean DEBUG = false;
    static final String TAG = "SignalClusterView";

    NetworkController mNC;
    private SettingsObserver mObserver;

    private static final int SIGNAL_CLUSTER_STYLE_NORMAL = 0;

    private int mSignalClusterStyle;
    private boolean mWifiVisible = false;
    private int mWifiStrengthId = 0, mWifiActivityId = 0;
    private boolean mMobileVisible = false;
    private int mMobileStrengthId = 0, mMobileActivityId = 0, mMobileTypeId = 0;
    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;
    private boolean mEtherVisible = false;
    private int mEtherIconId = 0;
    private String mWifiDescription, mMobileDescription, mMobileTypeDescription, mEtherDescription;

    private boolean mTabletMode;

    ViewGroup mWifiGroup, mMobileGroup;
    ImageView mWifi, mMobile, mWifiActivity, mMobileActivity, mMobileType, mAirplane, mEther;
    View mSpacer;

    Handler mHandler;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_SIGNAL_TEXT), false, this, UserHandle.USER_ALL);
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mHandler = new Handler();

        mTabletMode = Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.TABLET_MODE, context.getResources().getBoolean(
                com.android.internal.R.bool.config_showTabletNavigationBar) ? 1 : 0,
                UserHandle.USER_CURRENT) == 1 &&
                Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.TABLET_SCALED_ICONS, 1, UserHandle.USER_CURRENT) == 1;

        mObserver = new SettingsObserver(mHandler);
    }

    public void setNetworkController(NetworkController nc) {
        if (DEBUG) Slog.d(TAG, "NetworkController=" + nc);
        mNC = nc;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mObserver.observe();

        mWifiGroup      = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi           = (ImageView) findViewById(R.id.wifi_signal);
        mWifiActivity   = (ImageView) findViewById(R.id.wifi_inout);
        mMobileGroup    = (ViewGroup) findViewById(R.id.mobile_combo);
        mMobile         = (ImageView) findViewById(R.id.mobile_signal);
        mMobileActivity = (ImageView) findViewById(R.id.mobile_inout);
        mMobileType     = (ImageView) findViewById(R.id.mobile_type);
        mSpacer         =             findViewById(R.id.spacer);
        mAirplane       = (ImageView) findViewById(R.id.airplane);
        mEther          = (ImageView) findViewById(R.id.ethernet);

        apply();
    }

    @Override
    protected void onDetachedFromWindow() {
        mObserver.unobserve();

        mWifiGroup      = null;
        mWifi           = null;
        mWifiActivity   = null;
        mMobileGroup    = null;
        mMobile         = null;
        mMobileActivity = null;
        mMobileType     = null;
        mSpacer         = null;
        mAirplane       = null;
        mEther          = null;

        super.onDetachedFromWindow();
    }

    @Override
    public void setWifiIndicators(boolean visible, int strengthIcon, int activityIcon,
            String contentDescription) {
        mWifiVisible = visible;
        mWifiStrengthId = strengthIcon;
        mWifiActivityId = activityIcon;
        mWifiDescription = contentDescription;

        apply();
    }

    @Override
    public void setMobileDataIndicators(boolean visible, int strengthIcon, int activityIcon,
            int typeIcon, String contentDescription, String typeContentDescription) {
        mMobileVisible = visible;
        mMobileStrengthId = strengthIcon;
        mMobileActivityId = activityIcon;
        mMobileTypeId = typeIcon;
        mMobileDescription = contentDescription;
        mMobileTypeDescription = typeContentDescription;

        apply();
    }

    @Override
    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;

        apply();
    }

    @Override
    public void setEtherIndicators(boolean visible, int etherIcon, String contentDescription) {
        mEtherVisible = visible;
        mEtherIconId = etherIcon;
        mEtherDescription = contentDescription;

        apply();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Standard group layout onPopulateAccessibilityEvent() implementations
        // ignore content description, so populate manually
        if (mWifiVisible && mWifiGroup.getContentDescription() != null)
            event.getText().add(mWifiGroup.getContentDescription());
        if (mMobileVisible && mMobileGroup.getContentDescription() != null)
            event.getText().add(mMobileGroup.getContentDescription());
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        if (mWifi != null) {
            mWifi.setImageDrawable(null);
        }
        if (mWifiActivity != null) {
            mWifiActivity.setImageDrawable(null);
        }

        if (mMobile != null) {
            mMobile.setImageDrawable(null);
        }
        if (mMobileActivity != null) {
            mMobileActivity.setImageDrawable(null);
        }
        if (mMobileType != null) {
            mMobileType.setImageDrawable(null);
        }

        if(mAirplane != null) {
            mAirplane.setImageDrawable(null);
        }

        apply();
    }

    // Run after each indicator change.
    private void apply() {
        if (mWifiGroup == null) return;

        if (mWifiVisible) {
            mWifi.setImageResource(mWifiStrengthId);
            mWifiActivity.setImageResource(mWifiActivityId);

            mWifiGroup.setContentDescription(mWifiDescription);
            mWifiGroup.setVisibility(View.VISIBLE);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (DEBUG) Slog.d(TAG,
                String.format("wifi: %s sig=%d act=%d",
                    (mWifiVisible ? "VISIBLE" : "GONE"),
                    mWifiStrengthId, mWifiActivityId));

        if (mMobileVisible && !mIsAirplaneMode) {
            mMobile.setImageResource(mMobileStrengthId);
            mMobileActivity.setImageResource(mMobileActivityId);
            mMobileType.setImageResource(mMobileTypeId);

            mMobileGroup.setContentDescription(mMobileTypeDescription + " " + mMobileDescription);
            mMobileGroup.setVisibility(View.VISIBLE);
        } else {
            mMobileGroup.setVisibility(View.GONE);
        }

        if (mIsAirplaneMode) {
            mAirplane.setImageResource(mAirplaneIconId);
            mAirplane.setVisibility(View.VISIBLE);
        } else {
            mAirplane.setVisibility(View.GONE);
        }

        if (mEtherVisible) {
            mEther.setVisibility(View.VISIBLE);
            mEther.setImageResource(mEtherIconId);
            mEther.setContentDescription(mEtherDescription);
        } else {
            if (mEther != null) mEther.setVisibility(View.GONE);
        }

        if (mMobileVisible && mWifiVisible && mIsAirplaneMode) {
            mSpacer.setVisibility(View.INVISIBLE);
        } else {
            mSpacer.setVisibility(View.GONE);
        }

        if (DEBUG) Slog.d(TAG,
                String.format("mobile: %s sig=%d act=%d typ=%d",
                    (mMobileVisible ? "VISIBLE" : "GONE"),
                    mMobileStrengthId, mMobileActivityId, mMobileTypeId));

        mMobileType.setVisibility(
                !mWifiVisible ? View.VISIBLE : View.GONE);

        updateSettings();

        if (mTabletMode) {
            if (mWifi != null && mWifiGroup.getVisibility() == View.VISIBLE) scaleImage(mWifi, true);
            if (mWifiActivity != null && mWifiGroup.getVisibility() == View.VISIBLE) scaleImage(mWifiActivity, true);
            if (mMobile != null && mMobileGroup.getVisibility() == View.VISIBLE) scaleImage(mMobile, true);
            if (mMobileActivity != null && mMobileGroup.getVisibility() == View.VISIBLE) scaleImage(mMobileActivity, true);
            if (mMobileType != null && mMobileGroup.getVisibility() == View.VISIBLE) scaleImage(mMobileType, true);
            if (mAirplane != null && mAirplane.getVisibility() == View.VISIBLE) scaleImage(mAirplane, false);
        }
    }

    private void scaleImage(final ImageView view, final boolean frameLayout) {
        final float scale = (4f / 3f) * (float)
                        Settings.System.getIntForUser(mContext.getContentResolver(),
                        Settings.System.TABLET_HEIGHT, 100, UserHandle.USER_CURRENT) / 100f;
        int finalHeight = 0;
        int finalWidth = 0;
        int res = 0;
        if (view == mWifi) res = mWifiStrengthId;
        if (view == mMobile) res = mMobileStrengthId;
        if (view == mAirplane) res = mAirplaneIconId;
        if (view == mWifiActivity) res = mWifiActivityId;
        if (view == mMobileActivity) res = mMobileActivityId;
        if (view == mMobileType) res = mMobileTypeId;
        if (res != 0) {
            Drawable temp = getResources().getDrawable(res);
            if (temp != null) {
                finalHeight = temp.getIntrinsicHeight();
                finalWidth = temp.getIntrinsicWidth();
            }
        }
        if (frameLayout) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
            params.width = (int) (finalWidth * scale);
            params.height = (int) (finalHeight * scale);
            view.setLayoutParams(params);
        } else {
            LinearLayout.LayoutParams linParams = (LinearLayout.LayoutParams) view.getLayoutParams();
            linParams.width = (int) (finalWidth * scale);
            linParams.height = (int) (finalHeight * scale);
            view.setLayoutParams(linParams);
        }
    }

    private void updateSignalClusterStyle() {
        if (!mIsAirplaneMode) {
            mMobileGroup.setVisibility(mSignalClusterStyle !=
                    SIGNAL_CLUSTER_STYLE_NORMAL ? View.GONE : View.VISIBLE);
        }
    }

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mSignalClusterStyle = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_SIGNAL_TEXT, SIGNAL_CLUSTER_STYLE_NORMAL,
                UserHandle.USER_CURRENT);
        updateSignalClusterStyle();
    }
}

