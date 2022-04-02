package com.reverse.my.xploader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import de.robv.android.tmpd.IXposedHookLoadPackage;
import de.robv.android.tmpd.XC_MethodHook;
import de.robv.android.tmpd.XposedBridge;
import de.robv.android.tmpd.XposedHelpers;
import de.robv.android.tmpd.callbacks.XC_LoadPackage;

import static de.robv.android.tmpd.XposedHelpers.findAndHookConstructor;
import static de.robv.android.tmpd.XposedHelpers.findAndHookMethod;

/**
 * Created by maiyao on 2018/6/13.
 */

public class XpLoader implements IXposedHookLoadPackage {

    private static String TAG = "XpLoader";
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        //app启动时调用
        String pluginPkgName = "com.reverse.my.reverseutils";
        String cmd = String.format("pm path %s", pluginPkgName);
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            BufferedReader bufrIn = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            String line = bufrIn.readLine();
            bufrIn.close();
            if (line != null && !line.isEmpty()) {
                line = line.trim();
                String[] sa = line.split(":");
                String path = "";
                if (sa.length > 1) {
                    path = sa[1];
                } else if (sa.length == 1) {
                    path = sa[0];
                } else {
                    Log.w(TAG, String.format("can not find pkg %s", pluginPkgName));
                    return;
                }
                ZipFile zf = new ZipFile(path);
                ZipEntry ze = zf.getEntry("assets/xposed_init");
                if (ze == null) {
                    Log.w(TAG, String.format("can not find xposed_init in pkg path %s, ", path));
                    return;
                }
                InputStream is = zf.getInputStream(ze);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String classNameEntry = reader.readLine();
                reader.close();
                zf.close();
                if (classNameEntry == null || classNameEntry.isEmpty()) {
                    Log.w(TAG, String.format("init class is empty in pkg path %s, ", path));
                    return;
                }
                //通过PathClassLoader 加载apk
                classNameEntry = classNameEntry.trim();
                ClassLoader loader = XposedBridge.BOOTCLASSLOADER;
                //ClassLoader.getSystemClassLoader()
                final PathClassLoader pathClassLoader = new PathClassLoader(path, loader);
                try {
                    //反射调用插件的handleLoadPackage方法
                    final Class<?> aClass = Class.forName(classNameEntry, true, pathClassLoader);
                    final Method aClassMethod = aClass.getMethod("handleLoadPackage", XC_LoadPackage.LoadPackageParam.class);
                    aClassMethod.invoke(aClass.newInstance(), lpparam);
                } catch (InvocationTargetException e) {
                    Throwable e2 = e.getCause();
                    Log.e(TAG, String.format("load %s classNameEntry %s get InvocationTargetException msg %s", path, classNameEntry, e2.getMessage()));
                    Log.e(TAG, Log.getStackTraceString(e2));
                } catch (Exception e) {
                    Log.e(TAG, String.format("load %s classNameEntry %s error msg %s", path, classNameEntry, e.getMessage()));
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
            else {
                Log.w(TAG, String.format("can not find pkg %s, pm path return empty", pluginPkgName));
            }
        }
        catch (Throwable e) {
            Log.e(TAG, "xploader get exception:");
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}

