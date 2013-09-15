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

package com.android.systemui.statusbar.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.Slog;
import android.view.IWindowManager;

import com.android.systemui.settings.ToggleSlider;

public class VolumeController implements ToggleSlider.Listener {
    private static final String TAG = "StatusBar.VolumeController";
    private static final int STREAM_MUSIC = AudioManager.STREAM_MUSIC;
    private static final int STREAM_NOTIF = AudioManager.STREAM_NOTIFICATION;

    private Context mContext;
    private AudioManager mAudioManager;

    public VolumeController(Context context, ToggleSlider control) {
        mContext = context;
        mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        int volume = mAudioManager.getStreamVolume(mAudioManager.isMusicActive() ? STREAM_MUSIC : STREAM_NOTIF);
        boolean mute = volume < 1;
        control.setMax(mAudioManager.getStreamMaxVolume(mAudioManager.isMusicActive() ? STREAM_MUSIC : STREAM_NOTIF));
        control.setValue(volume);
        control.setChecked(mute);
        control.setOnChangedListener(this);
    }

    @Override
    public void onInit(ToggleSlider control) {
    }

    public void onChanged(ToggleSlider view, boolean tracking, boolean mute, int level) {
        if (!tracking) {
            if (mute) {
                mAudioManager.setStreamVolume(mAudioManager.isMusicActive() ? STREAM_MUSIC :
                        STREAM_NOTIF, 0, AudioManager.FLAG_PLAY_SOUND);
            } else {
                mAudioManager.setStreamVolume(mAudioManager.isMusicActive() ? STREAM_MUSIC :
                        STREAM_NOTIF, level, AudioManager.FLAG_PLAY_SOUND);
            }
        }
    }
}
