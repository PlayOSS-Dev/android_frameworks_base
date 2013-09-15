/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.systemui.statusbar.tablet;

import android.app.ActivityManagerNative;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.AirplaneModeController;
import com.android.systemui.statusbar.policy.AutoRotateController;
import com.android.systemui.statusbar.policy.BrightnessController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.DoNotDisturbController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.statusbar.policy.VolumeController;
import com.android.systemui.statusbar.policy.WifiController;

public class SettingsView extends LinearLayout implements View.OnClickListener,
        View.OnLongClickListener {
    static final String TAG = "SettingsView";

    public static final String BUTTON_WIFI = "toggleWifi";
    public static final String BUTTON_BLUETOOTH = "toggleBluetooth";
    public static final String BUTTON_BRIGHTNESS = "toggleBrightness";
    public static final String BUTTON_SOUND = "toggleSound";
    public static final String BUTTON_NOTIFICATIONS = "toggleNotifications";
    public static final String BUTTON_SETTINGS = "toggleSettings";
    public static final String BUTTON_AUTOROTATE = "toggleAutoRotate";
    public static final String BUTTON_AIRPLANE = "toggleAirplane";
    public static final String BUTTON_MEDIA = "toggleMedia";
    public static final String BUTTON_SLEEP = "toggleSleep";
    public static final String BUTTON_LOCATION = "toggleLocation";
    public static final String BUTTON_FLASHLIGHT = "toggleFlashlight";
    public static final String BUTTON_DELIMITER = "|";
    public static final String BUTTONS_DEFAULT =
            BUTTON_BRIGHTNESS + BUTTON_DELIMITER +
            BUTTON_SOUND + BUTTON_DELIMITER +
            BUTTON_AUTOROTATE + BUTTON_DELIMITER +
            BUTTON_NOTIFICATIONS + BUTTON_DELIMITER +
            BUTTON_SETTINGS;

    public static final int WIFI = 1;
    public static final int BLUETOOTH = 2;
    public static final int SETTINGS = 3;
    public static final int MEDIA_PREVIOUS = 5;
    public static final int MEDIA_PAUSE = 6;
    public static final int MEDIA_PLAY = 7;
    public static final int MEDIA_NEXT = 8;
    public static final int BRIGHTNESS = 14;
    public static final int SLEEP = 15;
    public static final int AUTOROTATE = 16;
    public static final int MEDIA_VOLUME = 17;
    public static final int LOCATION = 18;
    public static final int FLASHLIGHT = 19;

    AirplaneModeController mAirplane;
    AutoRotateController mRotate;
    BrightnessController mBrightness;
    DoNotDisturbController mDoNotDisturb;
    BluetoothController mBluetooth;
    FlashlightController mFlashlight;
    LocationController mGps;
    WifiController mWifi;
    VolumeController mVolume;
    TextView mSleepText;

    boolean mAirplaneInitialized = false;
    boolean mDNDInitialized = false;

    public SettingsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final Context context = getContext();

        String rows = Settings.System.getStringForUser(context.getContentResolver(),
                Settings.System.COMBINED_BAR_SETTINGS, UserHandle.USER_CURRENT);

        if (rows == null) {
            rows = BUTTONS_DEFAULT;
        }

        setupRows(context, rows);
    }

    private void setupRows(Context context, String rows) {
        final float scale = context.getResources().getDisplayMetrics().density;
        String[] settingsRow = rows.split("\\|");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (64 * scale));
        LinearLayout.LayoutParams iconlp = new LinearLayout.LayoutParams(
                (int) (64 * scale), ViewGroup.LayoutParams.MATCH_PARENT);
        iconlp.setMargins((int) (12 * scale), (int) (12 * scale), (int) (12 * scale), (int) (12 * scale));
        LinearLayout.LayoutParams separatorlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (1 * scale));
        LinearLayout.LayoutParams buttonseparatorlp = new LinearLayout.LayoutParams(1,
                ViewGroup.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams textlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textlp.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        LinearLayout.LayoutParams sliderlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        sliderlp.setMargins(0, 0, (int) (2 * scale), 0);
        LinearLayout.LayoutParams switchlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        switchlp.gravity = Gravity.CENTER_VERTICAL;
        switchlp.setMargins(0, 0, (int) (5 * scale), 0);
        View initialSeparator = new View(context);
        initialSeparator.setBackgroundResource(com.android.internal.R.drawable.divider_horizontal_dark);
        addView(initialSeparator, separatorlp);
        for (int i = 0; i < settingsRow.length; i++) {
            LinearLayout ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.HORIZONTAL);
            ll.setPadding(0, 0, (int) (16 * scale), 0);
            ll.setBackgroundResource(R.drawable.expanded_settings_background);

            ImageView icon = new ImageView(context);
            icon.setScaleType(ImageView.ScaleType.CENTER);
            if (settingsRow[i].contains(BUTTON_WIFI)) {
                icon.setImageResource(R.drawable.ic_sysbar_wifi_on);
                ll.addView(icon, iconlp);
                ll.addView(makeTextView(R.string.status_bar_settings_wifi_button), textlp);
                Switch toggle = new Switch(context);
                toggle.setGravity(Gravity.CENTER_VERTICAL);
                mWifi = new WifiController(context, toggle);
                ll.addView(toggle, switchlp);
                ll.setId(WIFI);
                ll.setOnLongClickListener(this);
            } else if (settingsRow[i].contains(BUTTON_BLUETOOTH)) {
                icon.setImageResource(R.drawable.ic_qs_bluetooth_off);
                ll.addView(icon, iconlp);
                ll.addView(makeTextView(R.string.status_bar_settings_bluetooth_button), textlp);
                Switch toggle = new Switch(context);
                toggle.setGravity(Gravity.CENTER_VERTICAL);
                mBluetooth = new BluetoothController(context, toggle);
                ll.addView(toggle, switchlp);
                ll.setId(BLUETOOTH);
                ll.setOnLongClickListener(this);
            } else if (settingsRow[i].contains(BUTTON_BRIGHTNESS)) {
                icon.setImageResource(R.drawable.ic_sysbar_brightness);
                ll.addView(icon, iconlp);
                ToggleSlider toggle = new ToggleSlider(context);
                toggle.setLabel(R.string.status_bar_settings_auto_brightness_label);
                mBrightness = new BrightnessController(context, toggle);
                ll.addView(toggle, sliderlp);
                ll.setId(BRIGHTNESS);
                ll.setOnLongClickListener(this);
            } else if (settingsRow[i].contains(BUTTON_SOUND)) {
                icon.setImageResource(R.drawable.ic_qs_ring_on);
                ll.addView(icon, iconlp);
                ToggleSlider toggle = new ToggleSlider(context);
                toggle.setLabel(R.string.status_bar_settings_mute_label);
                mVolume = new VolumeController(context, toggle);
                ll.addView(toggle, sliderlp);
                ll.setId(MEDIA_VOLUME);
                ll.setOnLongClickListener(this);
            } else if (settingsRow[i].contains(BUTTON_SLEEP)) {
                icon.setImageResource(R.drawable.ic_qs_screen_timeout_off);
                ll.addView(icon, iconlp);
                mSleepText = makeTextView(0);
                ll.addView(mSleepText, textlp);
                ll.setId(SLEEP);
                ll.setOnClickListener(this);
                ll.setOnLongClickListener(this);
                updateSleepSummary();
            } else if (settingsRow[i].contains(BUTTON_NOTIFICATIONS)) {
                icon.setImageResource(R.drawable.ic_notification_open);
                ll.addView(icon, iconlp);
                ll.addView(makeTextView(R.string.status_bar_settings_notifications), textlp);
                Switch toggle = new Switch(context);
                toggle.setGravity(Gravity.CENTER_VERTICAL);
                mDoNotDisturb = new DoNotDisturbController(context, toggle);
                mDNDInitialized = true;
                ll.addView(toggle, switchlp);
            } else if (settingsRow[i].contains(BUTTON_SETTINGS)) {
                icon.setImageResource(R.drawable.ic_sysbar_quicksettings);
                ll.addView(icon, iconlp);
                ll.addView(makeTextView(R.string.status_bar_settings_settings_button), textlp);
                ll.setId(SETTINGS);
                ll.setOnClickListener(this);
            } else if (settingsRow[i].contains(BUTTON_AUTOROTATE)) {
                icon.setImageResource(R.drawable.ic_sysbar_rotate_on);
                ll.addView(icon, iconlp);
                ll.addView(makeTextView(R.string.status_bar_settings_auto_rotation), textlp);
                Switch toggle = new Switch(context);
                toggle.setGravity(Gravity.CENTER_VERTICAL);
                mRotate = new AutoRotateController(context, toggle);
                ll.addView(toggle, switchlp);
                ll.setId(AUTOROTATE);
                ll.setOnLongClickListener(this);
            } else if (settingsRow[i].contains(BUTTON_LOCATION)) {
                icon.setImageResource(R.drawable.ic_qs_gps_neutral);
                ll.addView(icon, iconlp);
                ll.addView(makeTextView(R.string.status_bar_settings_location), textlp);
                Switch toggle = new Switch(context);
                toggle.setGravity(Gravity.CENTER_VERTICAL);
                mGps = new LocationController(context, toggle);
                ll.addView(toggle, switchlp);
            } else if (settingsRow[i].contains(BUTTON_FLASHLIGHT)) {
                icon.setImageResource(R.drawable.ic_qs_torch_off);
                ll.addView(icon, iconlp);
                ll.addView(makeTextView(R.string.status_bar_settings_flashlight), textlp);
                Switch toggle = new Switch(context);
                toggle.setGravity(Gravity.CENTER_VERTICAL);
                mFlashlight = new FlashlightController(context, toggle);
                ll.addView(toggle, switchlp);
            } else if (settingsRow[i].contains(BUTTON_AIRPLANE)) {
                icon.setImageResource(R.drawable.ic_sysbar_airplane_on);
                ll.addView(icon, iconlp);
                ll.addView(makeTextView(R.string.status_bar_settings_airplane), textlp);
                Switch toggle = new Switch(context);
                toggle.setGravity(Gravity.CENTER_VERTICAL);
                mAirplane = new AirplaneModeController(context, toggle);
                mAirplaneInitialized = true;
                ll.addView(toggle, switchlp);
            }

            addView(ll, lp);
            View separator = new View(context);
            separator.setBackgroundResource(com.android.internal.R.drawable.divider_horizontal_dark);
            addView(separator, separatorlp);
        }
    }

    private TextView makeTextView(int resource) {
        TextView text = new TextView(getContext());
        if (resource != 0) text.setText(resource);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setTextSize(18);
        text.setTextColor(0xfff3f3f3);
        return text;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAirplane != null && mAirplaneInitialized) {
            mAirplane.release();
            mAirplaneInitialized = false;
        }
        if (mDoNotDisturb != null && mDNDInitialized) {
            mDoNotDisturb.release();
            mDNDInitialized = false;
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case SETTINGS:
                onClickSettings();
                break;
            case SLEEP:
                onClickSleep();
                break;
            case MEDIA_PREVIOUS:
                onClickMedia(MEDIA_PREVIOUS);
                break;
            case MEDIA_PAUSE:
                onClickMedia(MEDIA_PAUSE);
                break;
            case MEDIA_PLAY:
                onClickMedia(MEDIA_PLAY);
                break;
            case MEDIA_NEXT:
                onClickMedia(MEDIA_NEXT);
                break;
        }
    }

    public boolean onLongClick(View v) {
        try {
            ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
        } catch (RemoteException e) {
        }
        switch (v.getId()) {
            case WIFI:
                onClickNetwork();
                return true;
            case BLUETOOTH:
                onClickBluetooth();
                return true;
            case SLEEP:
            case BRIGHTNESS:
            case AUTOROTATE:
                onClickDisplay();
                return true;
            case MEDIA_VOLUME:
                onClickVolume();
                return true;
        }
        return false;
    }

    private StatusBarManager getStatusBarManager() {
        return (StatusBarManager)getContext().getSystemService(Context.STATUS_BAR_SERVICE);
    }

    // Network
    // ----------------------------
    private void onClickNetwork() {
        getContext().startActivityAsUser(new Intent(Settings.ACTION_WIFI_SETTINGS)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), new UserHandle(UserHandle.USER_CURRENT));
        getStatusBarManager().collapsePanels();
    }

    // Bluetooth
    // ----------------------------
    private void onClickBluetooth() {
        getContext().startActivityAsUser(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), new UserHandle(UserHandle.USER_CURRENT));
        getStatusBarManager().collapsePanels();
    }

    // Settings
    // ----------------------------
    private void onClickSettings() {
        try {
            ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
        } catch (RemoteException e) {
        }
        getContext().startActivityAsUser(new Intent(Settings.ACTION_SETTINGS)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), new UserHandle(UserHandle.USER_CURRENT));
        getStatusBarManager().collapsePanels();
    }

    // Media
    // ----------------------------
    private void onClickMedia(int index) {
        long eventtime = SystemClock.uptimeMillis();
        int code = 0;

        switch (index) {
            case MEDIA_PREVIOUS:
                code = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
                break;
            case MEDIA_PAUSE:
                code = KeyEvent.KEYCODE_MEDIA_PAUSE;
                break;
            case MEDIA_PLAY:
                code = KeyEvent.KEYCODE_MEDIA_PLAY;
                break;
            case MEDIA_NEXT:
                code = KeyEvent.KEYCODE_MEDIA_NEXT;
                break;
        }

        Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, code, 0);
        downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
        getContext().sendOrderedBroadcast(downIntent, null);

        Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, code, 0);
        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
        getContext().sendOrderedBroadcast(upIntent, null);
    }

    // Display
    // ----------------------------
    private void onClickDisplay() {
        Intent intent = new Intent("android.settings.DISPLAY_SETTINGS");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
        getStatusBarManager().collapsePanels();
    }

    // Volume
    // ----------------------------
    private void onClickVolume() {
        Intent intent = new Intent("android.settings.SOUND_SETTINGS");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
        getStatusBarManager().collapsePanels();
    }

    // Sleep
    // ----------------------------
    private void onClickSleep() {
        int value = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, 60000, UserHandle.USER_CURRENT);
        String[] entries = mContext.getResources().getStringArray(R.array.screen_timeout_entries);
        String[] values = mContext.getResources().getStringArray(R.array.screen_timeout_values);
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            int timeout = Integer.parseInt(values[i].toString());
            if (value >= timeout) {
                best = i;
            }
        }
        value = Integer.parseInt(values[values.length - 1 == best ? 0 : best + 1].toString());
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, value, UserHandle.USER_CURRENT);
        updateSleepSummary();
    }

    private void updateSleepSummary() {
        int value = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, 60000, UserHandle.USER_CURRENT);
        String[] entries = mContext.getResources().getStringArray(R.array.screen_timeout_entries);
        String[] values = mContext.getResources().getStringArray(R.array.screen_timeout_values);
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            int timeout = Integer.parseInt(values[i].toString());
            if (value >= timeout) {
                best = i;
            }
        }
        mSleepText.setText(mContext.getString(R.string.status_bar_screen_timeout,
                entries[best]));
    }
}
