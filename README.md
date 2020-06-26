- An Xposed plugin loader.just compile and install it as xposed module, It will load an xposed like plugin with fixed package name com.reverse.my.reverseutils.
This plugin can update and work without rebooting the phone, can use normal xposed api, but notices that this plugin SHOULD NOT BE a xposed module, which means you should NOT write following in the AndroidManifest.xml

```
        <!-- meta-data android:name="xposedmodule" android:value="true" /-->

        <!-- meta-data android:name="xposeddescription" android:value="HookDemo" /-->

        <!-- meta-data android:name="xposedminversion" android:value="53" /-->
```
