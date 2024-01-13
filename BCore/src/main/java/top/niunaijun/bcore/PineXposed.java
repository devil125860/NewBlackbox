package top.niunaijun.bcore;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import top.canyie.pine.PineConfig;
import top.niunaijun.bcore.app.BActivityThread;
import top.niunaijun.bcore.core.system.user.BUserHandle;
import top.niunaijun.bcore.utils.MethodParameterUtils;
import top.niunaijun.bcore.utils.compat.BuildCompat;
import top.niunaijun.black_fake.BuildConfig;

/**
 * @author gm
 * @function
 * @date :2024/1/13 17:20
 **/
public class PineXposed {

    public static void init(){
        PineConfig.debug = true; // Do we need to print more detailed logs?
        PineConfig.debuggable = BuildConfig.DEBUG; // Is this process debuggable?
    }

    private static XC_MethodHook PendingIntentFlagsHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            int flags = ((Integer) param.args[3]).intValue();
            if ((flags & 0x04000000) == 0 && (0x02000000 & flags) == 0) {
                param.args[3] = Integer.valueOf(0x04000000 | flags);
            }
        }
    };

    public static void fixPendingIntentFlags(Context context) {
        if (BuildCompat.isS()) {
            XposedHelpers.findAndHookMethod(PendingIntent.class, "getActivityAsUser", new Object[]{Context.class, Integer.TYPE, Intent.class, Integer.TYPE, Bundle.class, UserHandle.class, PendingIntentFlagsHook});
            XposedHelpers.findAndHookMethod(PendingIntent.class, "getActivitiesAsUser", new Object[]{Context.class, Integer.TYPE, Intent[].class, Integer.TYPE, Bundle.class, UserHandle.class, PendingIntentFlagsHook});
            XposedHelpers.findAndHookMethod(PendingIntent.class, "getBroadcastAsUser", new Object[]{Context.class, Integer.TYPE, Intent.class, Integer.TYPE, UserHandle.class, PendingIntentFlagsHook});
            XposedHelpers.findAndHookMethod(PendingIntent.class, "getForegroundService", new Object[]{Context.class, Integer.TYPE, Intent.class, Integer.TYPE, PendingIntentFlagsHook});
        }
    }

    public static void initForXposed(Context context, String processName) {
        fixPendingIntentFlags(context);
        if (BuildCompat.isS()) {
            try {
                XposedHelpers.findAndHookMethod("android.content.AttributionSource", context.getClassLoader(), "checkCallingUid", new Object[]{new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        return true;
                    }
                }});
            } catch (Throwable th) {
            }
        }
        try {
            XposedBridge.hookAllMethods(MediaRecorder.class, "native_setup", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    MethodParameterUtils.replaceFirstAppPkg(param.args);
                }
            });
        } catch (Throwable throwable) {

        }
        try {
            XposedHelpers.findAndHookMethod("android.view.WindowManagerGlobal", context.getClassLoader(), "addView", new Object[]{View.class, ViewGroup.LayoutParams.class, Display.class, Window.class, Integer.TYPE, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    param.args[4] = Integer.valueOf(BActivityThread.getUserId());
                }
            }});
        } catch (Throwable th2) {
        }
    }
}
