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
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

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
            String line = bufrIn.readLine().trim();
            if (!line.isEmpty()) {
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
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String classNameEntry = reader.readLine().trim();
                reader.close();
                zf.close();
                if (classNameEntry.isEmpty()) {
                    Log.w(TAG, String.format("init class is empty in pkg path %s, ", path));
                    return;
                }
                //通过PathClassLoader 加载apk
                final PathClassLoader pathClassLoader = new PathClassLoader(path, ClassLoader.getSystemClassLoader());
                //通过反射调用 MainModule的 handleLoadPackage 方法【重点】
                final Class<?> aClass;
                try {
                    //反射调用MainModule的handleLoadPackage方法
                    aClass = Class.forName(classNameEntry, true, pathClassLoader);
                    final Method aClassMethod = aClass.getMethod("handleLoadPackage", XC_LoadPackage.LoadPackageParam.class);
                    aClassMethod.invoke(aClass.newInstance(), lpparam);
                } catch (Exception e) {
                    Log.e(TAG, String.format("load %s error msg %s", path, e.getMessage()));
                    Log.e(TAG, Log.getStackTraceString(e));
                }


                /*
                //通过PathClassLoader 加载apk
                final PathClassLoader pathClassLoader = new PathClassLoader(path, ClassLoader.getSystemClassLoader());
                String className = MainModule.class.getName();
                //通过反射调用 MainModule的 handleLoadPackage 方法【重点】
                final Class<?> aClass;
                try {
                    //反射调用MainModule的handleLoadPackage方法
                    aClass = Class.forName(className, true, pathClassLoader);
                    final Method aClassMethod = aClass.getMethod("handleLoadPackage", XC_LoadPackage.LoadPackageParam.class);
                    aClassMethod.invoke(aClass.newInstance(), param);
                } catch (Exception e) {
                    LogUtil.e("反射M ainModule 失败："+e.getMessage());
                }
                 */
            }
            else {
                Log.w(TAG, String.format("can not find pkg %s, pm path return empty", pluginPkgName));
            }
        }
        catch (Throwable e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}

