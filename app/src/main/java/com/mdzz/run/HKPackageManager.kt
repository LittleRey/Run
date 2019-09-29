package com.mdzz.run

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.mdzz.run.base.BaseHook
import com.mdzz.run.filter.ApplicationInfoFilter
import com.mdzz.run.filter.Filter
import com.mdzz.run.filter.PackageInfoFilter
import com.mdzz.run.util.XSharedPrefUtil
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

@Suppress("UNCHECKED_CAST")
class HKPackageManager : BaseHook() {

    companion object {
        private const val TAG = "HKPackageManager"
    }

    @SuppressLint("PrivateApi")
    override fun beginHook() {
        try {
            val appPackageManagerClass = classLoader.loadClass(PACKAGEMANAGER_CLASS)
            XposedHelpers.findAndHookMethod(appPackageManagerClass, "getPackageInfo",
                    String::class.java, Int::class.javaPrimitiveType, MyMethodHook)
            XposedHelpers.findAndHookMethod(appPackageManagerClass, "getApplicationInfo",
                    String::class.java, Int::class.javaPrimitiveType, MyMethodHook)
            XposedHelpers.findAndHookMethod(appPackageManagerClass, "getInstalledApplications",
                    Int::class.javaPrimitiveType, MyIAppMethodHook)
            XposedHelpers.findAndHookMethod(appPackageManagerClass, "getInstalledPackages",
                    Int::class.javaPrimitiveType, MyIPkgMethodHook)
            log(TAG, "run: 模块2工作正常")
        } catch (th: Throwable) {
            log(TAG, "run: 模块2出错")
            log(TAG, th)
        }
    }

    object MyIAppMethodHook : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val applicationInfos = param.result as List<ApplicationInfo>
            param.result = ApplicationInfoFilter.get(applicationInfos)
        }
    }

    object MyIPkgMethodHook : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val packageInfos = param.result as List<PackageInfo>
            param.result = PackageInfoFilter.get(packageInfos)
        }
    }

    object MyMethodHook : XC_MethodHook() {

        private val protectedPackageNames
                get() = XSharedPrefUtil.getStringSet(NEED_PROTECT_PACKAGE, delimiter = "\n")

        override fun beforeHookedMethod(param: MethodHookParam?) {
            param?.let {
                val packageName = it.args[0] as String
                if (packageName.startsWith("android.") || packageName.contains(HOOK_PACKAGE)) {
                    return
                }
                if (needProtected(packageName)) {
                    it.throwable = PackageManager.NameNotFoundException(packageName)
                    return
                }
            }
        }

        override fun afterHookedMethod(param: MethodHookParam?) {
            param?.let {
                val packageName = it.args[0] as String
                var shouldThrow = false
                if (hasResult(it)) {
                    when (it.result::class.java.simpleName) {
                        "ApplicationInfo" -> {
                            val applicationInfo = param.result as ApplicationInfo
                            if (Filter.isSystemApp(applicationInfo.flags)) {
                                return
                            } else {
                                shouldThrow = true
                            }
                        }
                        "PackageInfo" -> {
                            val packageInfo = it.result as PackageInfo
                            if (Filter.isSystemApp(packageInfo.applicationInfo.flags)) {
                                return
                            } else {
                                shouldThrow = true
                            }
                        }
                    }
                }
                if (shouldThrow) {
                    it.throwable = PackageManager.NameNotFoundException(packageName)
                }
            }
        }

        private fun hasResult(param: MethodHookParam): Boolean {
            return param.throwable == null
        }

        private fun needProtected(packageName: String) = packageName in protectedPackageNames
                || packageName == "de.robv.android.xposed.installer"
    }
}