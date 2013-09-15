/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.BluetoothStateChangeCallback;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class BluetoothController extends BroadcastReceiver
        implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "StatusBar.BluetoothController";

    private static final int mViewId = ImageView.generateViewId();

    private final BluetoothAdapter mAdapter;
    private Context mContext;
    private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
    private CompoundButton mCheckBox;
    private int mIconId = R.drawable.stat_sys_data_bluetooth;
    private int mContentDescriptionId = 0;
    private int mState = BluetoothAdapter.ERROR;
    private boolean mEnabled = false;

    private boolean mTabletMode;

    private Set<BluetoothDevice> mBondedDevices = new HashSet<BluetoothDevice>();

    private ArrayList<BluetoothStateChangeCallback> mChangeCallbacks =
            new ArrayList<BluetoothStateChangeCallback>();

    public BluetoothController(Context context) {
        mContext = context;

        mTabletMode = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.TABLET_MODE, mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_showTabletNavigationBar) ? 1 : 0,
                UserHandle.USER_CURRENT) == 1 &&
                Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.TABLET_SCALED_ICONS, 1, UserHandle.USER_CURRENT) == 1;

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(this, filter);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter != null) {
            handleAdapterStateChange(mAdapter.getState());
            handleConnectionStateChange(mAdapter.getConnectionState());
        }
        refreshViews();
        updateBondedBluetoothDevices();
    }

    public BluetoothController(Context context, CompoundButton checkbox) {
        mContext = context;
        mCheckBox = checkbox;
        mCheckBox.setChecked(mEnabled);
        mCheckBox.setOnCheckedChangeListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(this, filter);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter != null) {
            handleAdapterStateChange(mAdapter.getState());
            handleConnectionStateChange(mAdapter.getConnectionState());
        }
        refreshViews();
        updateBondedBluetoothDevices();
    }

    public void addPanelIconView(ImageView v) {
        mIconViews.add(v);
    }

    public void addIconView(ImageView v) {
        if (mTabletMode) v.setId(mViewId);

        mIconViews.add(v);
    }

    public void addStateChangedCallback(BluetoothStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
    }

    public void removeStateChangedCallback(BluetoothStateChangeCallback cb) {
        mChangeCallbacks.remove(cb);
    }

    public Set<BluetoothDevice> getBondedBluetoothDevices() {
        return mBondedDevices;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            handleAdapterStateChange(
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR));
        } else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
            handleConnectionStateChange(
                    intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                        BluetoothAdapter.STATE_DISCONNECTED));
        } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
            // Fall through and update bonded devices and refresh view
        }
        refreshViews();
        updateBondedBluetoothDevices();
    }

    private void updateBondedBluetoothDevices() {
        mBondedDevices.clear();

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            Set<BluetoothDevice> devices = adapter.getBondedDevices();
            if (devices != null) {
                for (BluetoothDevice device : devices) {
                    if (device.getBondState() != BluetoothDevice.BOND_NONE) {
                        mBondedDevices.add(device);
                    }
                }
            }
        }
    }

    public void handleAdapterStateChange(int adapterState) {
        mEnabled = (adapterState == BluetoothAdapter.STATE_ON);
    }

    public void handleConnectionStateChange(int connectionState) {
        final boolean connected = (connectionState == BluetoothAdapter.STATE_CONNECTED);
        if (connected) {
            mIconId = R.drawable.stat_sys_data_bluetooth_connected;
            mContentDescriptionId = R.string.accessibility_bluetooth_connected;
        } else {
            mIconId = R.drawable.stat_sys_data_bluetooth;
            mContentDescriptionId = R.string.accessibility_bluetooth_disconnected;
        }
    }

    public void refreshViews() {
        int N = mIconViews.size();
        for (int i=0; i<N; i++) {
            ImageView v = mIconViews.get(i);
            v.setImageResource(mIconId);
            v.setVisibility(mEnabled ? View.VISIBLE : View.GONE);
            v.setContentDescription((mContentDescriptionId == 0)
                    ? null
                    : mContext.getString(mContentDescriptionId));
            if (mTabletMode && v.getVisibility() == View.VISIBLE && v.getId() == mViewId) {
                scaleImage(v);
            }
        }
        for (BluetoothStateChangeCallback cb : mChangeCallbacks) {
            cb.onBluetoothStateChange(mEnabled);
        }
        if (mAdapter != null) setBluetoothStateInt(mAdapter.getState());
    }

    private void scaleImage(ImageView view) {
        final float scale = (4f / 3f) * (float)
                        Settings.System.getIntForUser(mContext.getContentResolver(),
                        Settings.System.TABLET_HEIGHT, 100, UserHandle.USER_CURRENT) / 100f;
        int finalHeight = 0;
        int finalWidth = 0;
        int res = mIconId;
        if (res != 0) {
            Drawable temp = view.getResources().getDrawable(res);
            if (temp != null) {
                finalHeight = temp.getIntrinsicHeight();
                finalWidth = temp.getIntrinsicWidth();
            }
        }
        LinearLayout.LayoutParams linParams = (LinearLayout.LayoutParams) view.getLayoutParams();
        linParams.width = (int) (finalWidth * scale + 4 * view.getResources().getDisplayMetrics().density);
        linParams.height = (int) (finalHeight * scale);
        view.setLayoutParams(linParams);
    }

    public void onCheckedChanged(CompoundButton view, boolean checked) {
        if (checked != mEnabled) {
            mEnabled = checked;
            setBluetoothEnabled(mEnabled);
            setBluetoothStateInt(mAdapter.getState());
            syncBluetoothState();
        }
    }

    public void setBluetoothEnabled(boolean enabled) {
        boolean success = enabled
                ? mAdapter.enable()
                : mAdapter.disable();

        if (success) {
            setBluetoothStateInt(enabled
				    ? BluetoothAdapter.STATE_TURNING_ON
                    : BluetoothAdapter.STATE_TURNING_OFF);
        } else {
	        syncBluetoothState();
        }
    }

    boolean syncBluetoothState() {
        int currentState = mAdapter.getState();
        if (currentState != mState) {
            setBluetoothStateInt(mState);
            return true;
        }
	return false;
}

    synchronized void setBluetoothStateInt(int state) {
        mState = state;
        if (state == BluetoothAdapter.STATE_ON) {
		    if (mCheckBox != null) {
		        mCheckBox.setChecked(true);
	        }
	    } else {
	        if (mCheckBox != null) {
                mCheckBox.setChecked(false);
            }
        }
    }

}
