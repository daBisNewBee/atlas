package android.taobao.atlas.bridge;

import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Process;
import android.support.multidex.MultiDex;
import android.taobao.atlas.framework.Atlas;
import android.taobao.atlas.framework.Framework;
import android.taobao.atlas.hack.AndroidHack;
import android.taobao.atlas.hack.AtlasHacks;
import android.taobao.atlas.hack.Hack.HackedField;
import android.taobao.atlas.hack.Hack.HackedMethod;
import android.taobao.atlas.runtime.AtlasPreLauncher;
import android.taobao.atlas.runtime.PackageManagerDelegate;
import android.taobao.atlas.runtime.RuntimeVariables;
import android.taobao.atlas.runtime.newcomponent.AdditionalActivityManagerProxy;
import android.taobao.atlas.startup.KernalVersionManager;
import android.taobao.atlas.util.AtlasCrashManager;
import android.taobao.atlas.util.SoLoader;
import android.taobao.atlas.util.log.IAlarmer;
import android.taobao.atlas.util.log.IMonitor;
import android.taobao.atlas.util.log.impl.AtlasAlarmer;
import android.taobao.atlas.util.log.impl.AtlasMonitor;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class BridgeApplicationDelegate
{
    private String mRealApplicationName;
    private Application mRealApplication;
    private Application mRawApplication;
    private String mInstalledVersionName;
    private String mCurrentProcessname;
    private long mInstalledVersionCode;
    private long mLastUpdateTime;
    private boolean mIsUpdated;
    private String mApkPath;
    private Object mdexLoadBooster;
    private List<ProviderInfo> mBoundApplication_provider;

    public BridgeApplicationDelegate(Application rawApplication, String processname, String installedVersion, long versioncode, long lastupdatetime, String apkPath, boolean isUpdated, Object dexLoadBooster)
    {
        this.mRawApplication = rawApplication;
        this.mCurrentProcessname = processname;
        this.mInstalledVersionName = installedVersion;
        this.mInstalledVersionCode = versioncode;
        this.mLastUpdateTime = lastupdatetime;
        this.mIsUpdated = isUpdated;
        this.mApkPath = apkPath;
        this.mdexLoadBooster = dexLoadBooster;
        PackageManagerDelegate.delegatepackageManager(rawApplication.getBaseContext());
    }

    public void attachBaseContext()
    {
        AtlasHacks.defineAndVerify();
        RuntimeVariables.androidApplication = this.mRawApplication;
        RuntimeVariables.originalResources = this.mRawApplication.getResources();
        RuntimeVariables.sCurrentProcessName = this.mCurrentProcessname;
        RuntimeVariables.sInstalledVersionCode = this.mInstalledVersionCode;
        RuntimeVariables.sAppLastUpdateTime = this.mLastUpdateTime;
        RuntimeVariables.sApkPath = this.mApkPath;
        RuntimeVariables.delegateResources = this.mRawApplication.getResources();
        RuntimeVariables.sDexLoadBooster = this.mdexLoadBooster;
        Log.e("BridgeApplication", "length =" + new File(this.mRawApplication.getApplicationInfo().sourceDir).length());
        if ((!Build.MANUFACTURER.equalsIgnoreCase("vivo")) || (Build.VERSION.SDK_INT != 23)) {
            try
            {
                RuntimeVariables.sDexLoadBooster.getClass().getDeclaredMethod("setVerificationEnabled", new Class[] { Boolean.TYPE }).invoke(RuntimeVariables.sDexLoadBooster, new Object[] { Boolean.valueOf(false) });
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
        }
        if (!TextUtils.isEmpty(this.mInstalledVersionName)) {
            RuntimeVariables.sInstalledVersionName = this.mInstalledVersionName;
        }
        AtlasCrashManager.forceStopAppWhenCrashed();
        System.out.print(SoLoader.class.getName());
        try
        {
            String preLaunchStr = (String)RuntimeVariables.getFrameworkProperty("preLaunch");
            if (!TextUtils.isEmpty(preLaunchStr))
            {
                AtlasPreLauncher launcher = (AtlasPreLauncher)Class.forName(preLaunchStr).newInstance();
                if (launcher != null) {
                    launcher.initBeforeAtlas(this.mRawApplication.getBaseContext());
                }
            }
        }
        catch (Throwable e)
        {
            throw new RuntimeException(e);
        }
        boolean multidexEnable = false;
        try
        {
            ApplicationInfo appInfo = this.mRawApplication.getPackageManager().getApplicationInfo(this.mRawApplication.getPackageName(), 128);

            this.mRealApplicationName = appInfo.metaData.getString("REAL_APPLICATION");
            multidexEnable = appInfo.metaData.getBoolean("multidex_enable");
        }
        catch (PackageManager.NameNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        if (multidexEnable) {
            MultiDex.install(this.mRawApplication);
        }
        this.mRealApplicationName = (TextUtils.isEmpty(this.mRealApplicationName) ? "android.app.Application" : this.mRealApplicationName);
        if (this.mRealApplicationName.startsWith(".")) {
            this.mRealApplicationName = (this.mRawApplication.getPackageName() + this.mRealApplicationName);
        }
        RuntimeVariables.sRealApplicationName = this.mRealApplicationName;
        try
        {
            Atlas.getInstance().init(this.mRawApplication, this.mIsUpdated);
        }
        catch (Throwable e)
        {
            File storageDir = new File(this.mRawApplication.getFilesDir(), "storage");
            Framework.deleteDirectory(storageDir);
            KernalVersionManager.instance().removeBaseLineInfo();
            Process.killProcess(Process.myPid());
        }
        try
        {
            Class BuildConfig = Class.forName(this.mRawApplication.getPackageName() + ".BuildConfig");
            Field launchTimeField = BuildConfig.getDeclaredField("launchTime");
            launchTimeField.setAccessible(true);
            launchTimeField.set(BuildConfig, Long.valueOf(System.currentTimeMillis()));
        }
        catch (Throwable localThrowable1) {}
        try
        {
            Object activityThread = AndroidHack.getActivityThread();
            Object mBoundApplication = AtlasHacks.ActivityThread_mBoundApplication.get(activityThread);
            this.mBoundApplication_provider = ((List)AtlasHacks.ActivityThread$AppBindData_providers.get(mBoundApplication));
            if ((this.mBoundApplication_provider != null) && (this.mBoundApplication_provider.size() > 0))
            {
                AtlasHacks.ActivityThread$AppBindData_providers.set(mBoundApplication, null);
            }
            else
            {
                this.mBoundApplication_provider = new ArrayList();
                if ((Build.VERSION.SDK_INT >= 24) && (this.mCurrentProcessname != null) && (this.mCurrentProcessname.equals(this.mRawApplication.getPackageName())))
                {
                    ProviderInfo providerInfo = this.mRawApplication.getPackageManager().resolveContentProvider(this.mRawApplication.getPackageName() + ".update.provider", 0);
                    if (providerInfo != null)
                    {
                        providerInfo.exported = false;
                        providerInfo.grantUriPermissions = true;
                        this.mBoundApplication_provider.add(providerInfo);
                    }
                }
                ProviderInfo providerInfo1 = this.mRawApplication.getPackageManager().resolveContentProvider(this.mRawApplication.getPackageName() + ".com.android.alibaba.ip.server.InstantRunContentProvider", 0);
                if (providerInfo1 != null)
                {
                    providerInfo1.exported = false;
                    providerInfo1.multiprocess = false;
                    this.mBoundApplication_provider.add(providerInfo1);
                }
            }
        }
        catch (Exception e)
        {
            if ((e instanceof InvocationTargetException)) {
                throw new RuntimeException(((InvocationTargetException)e).getTargetException());
            }
            throw new RuntimeException(e);
        }
    }

    public void onCreate()
    {
        try
        {
            AdditionalActivityManagerProxy.get().startRegisterReceivers(RuntimeVariables.androidApplication);

            this.mRealApplication = ((Application)this.mRawApplication.getBaseContext().getClassLoader().loadClass(this.mRealApplicationName).newInstance());

            Object activityThread = AndroidHack.getActivityThread();

            AtlasHacks.ContextImpl_setOuterContext.invoke(this.mRawApplication.getBaseContext(), new Object[] { this.mRealApplication });

            Object mPackageInfo = AtlasHacks.ContextImpl_mPackageInfo.get(this.mRawApplication.getBaseContext());
            AtlasHacks.LoadedApk_mApplication.set(mPackageInfo, this.mRealApplication);

            AtlasHacks.ActivityThread_mInitialApplication.set(activityThread, this.mRealApplication);

            List<Application> allApplications = (List)AtlasHacks.ActivityThread_mAllApplications.get(activityThread);
            for (int i = 0; i < allApplications.size(); i++) {
                if (allApplications.get(i) == this.mRawApplication) {
                    allApplications.set(i, this.mRealApplication);
                }
            }
            RuntimeVariables.androidApplication = this.mRealApplication;

            this.mRealApplication.registerComponentCallbacks(new ComponentCallbacks()
            {
                public void onConfigurationChanged(Configuration newConfig)
                {
                    DisplayMetrics newMetrics = new DisplayMetrics();
                    if ((RuntimeVariables.delegateResources != null) && (RuntimeVariables.androidApplication != null))
                    {
                        WindowManager manager = (WindowManager)RuntimeVariables.androidApplication.getSystemService("window");
                        if ((manager == null) || (manager.getDefaultDisplay() == null))
                        {
                            Log.e("BridgeApplication", "get windowmanager service failed");
                            return;
                        }
                        manager.getDefaultDisplay().getMetrics(newMetrics);
                        RuntimeVariables.delegateResources.updateConfiguration(newConfig, newMetrics);
                        try
                        {
                            Method method = Resources.class.getDeclaredMethod("updateSystemConfiguration", new Class[] { Configuration.class, DisplayMetrics.class,
                                    Class.forName("android.content.res.CompatibilityInfo") });
                            method.setAccessible(true);
                            method.invoke(RuntimeVariables.delegateResources, new Object[] { newConfig, newMetrics, null });
                        }
                        catch (Throwable e)
                        {
                            e.printStackTrace();
                        }
                    }
                }

                public void onLowMemory() {}
            });
            AtlasHacks.Application_attach.invoke(this.mRealApplication, new Object[] { this.mRawApplication.getBaseContext() });
            if ((this.mBoundApplication_provider != null) && (this.mBoundApplication_provider.size() > 0))
            {
                Object mBoundApplication = AtlasHacks.ActivityThread_mBoundApplication.get(activityThread);
                AtlasHacks.ActivityThread$AppBindData_providers.set(mBoundApplication, this.mBoundApplication_provider);
                try {
                    AtlasHacks.ActivityThread_installContentProviders.invoke(activityThread, new Object[] { this.mRealApplication, this.mBoundApplication_provider });

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        catch (Throwable e)
        {
            if ((e instanceof InvocationTargetException)) {
                throw new RuntimeException(((InvocationTargetException)e).getTargetException());
            }
            throw new RuntimeException(e);
        }
        if ((this.mRealApplication instanceof IMonitor))
        {
            AtlasMonitor.getInstance();AtlasMonitor.setExternalMonitor((IMonitor)this.mRealApplication);
        }
        if ((this.mRealApplication instanceof IAlarmer))
        {
            AtlasAlarmer.getInstance();AtlasAlarmer.setExternalAlarmer((IAlarmer)this.mRealApplication);
        }
        Atlas.getInstance().startup(this.mRealApplication, this.mIsUpdated);

        this.mRealApplication.onCreate();
    }
}
