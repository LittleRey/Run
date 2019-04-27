package com.mdzz.run

import com.mdzz.run.util.XSharedPrefUtil
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.mdzz.hook") {
            check(lpparam.classLoader)
            return
        }
        if (lpparam.packageName == HOOK_PACKAGE
                && lpparam.processName == HOOK_PACKAGE) {
            if (XSharedPrefUtil.getBoolean(HOOK_START)) {
                HKApplication().getClassLoaderAndStartHook(lpparam)
            }
        }
    }

    private fun check(classLoader: ClassLoader) {
        XposedHelpers.findAndHookMethod("com.mdzz.activity.MainActivity",
                classLoader, "isActive", XC_MethodReplacement.returnConstant(true))
    }
}