package com.android.systemui.quicksettings;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.VolumePanel;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.ImageView;

import com.android.internal.app.ThemeUtils;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsController;

public class AppDrawerTile extends QuickSettingsTile {
    public AppDrawerTile(Context context, final QuickSettingsController qsc) {
        super(context, qsc);

        mLabel = context.getString(R.string.quick_settings_appdrawer);
        mDrawable = R.drawable.ic_notify_quicksettings_normal;

        final Context clickContext = context;

        mOnClick = new OnClickListener() {

            @Override
            public void onClick(View v) {
                qsc.collapseAllPanels(true);
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.addCategory("android.intent.category.HOME");
                intent.addCategory("com.cyanogenmod.trebuchet.APP_DRAWER");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                clickContext.startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
            }
        };

        mOnLongClick = new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                qsc.collapseAllPanels(true);
                Intent launcherPreferencesIntent = new Intent();
                launcherPreferencesIntent.setClassName("com.cyanogenmod.trebuchet",
                        "com.cyanogenmod.trebuchet.preference.Preferences");
                clickContext.startActivityAsUser(launcherPreferencesIntent,
                        new UserHandle(UserHandle.USER_CURRENT));
                return true;
            }
        };
    }
}
