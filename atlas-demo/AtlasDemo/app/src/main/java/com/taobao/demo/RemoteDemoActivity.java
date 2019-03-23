package com.taobao.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.taobao.atlas.framework.Atlas;
import android.taobao.atlas.framework.Framework;
import android.widget.Button;

import org.osgi.framework.BundleException;

import java.io.File;


public class RemoteDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guild_remote_bundle);

        final Activity activity = this;

        Button button = (Button) findViewById(R.id.load_remote_bundle);
        button.setOnClickListener(view -> {

            // start：实现远端bundle的更新：先卸载，再加载。（默认只加载一次）
            String path = "/sdcard/Android/data/com.taobao.demo/cache/libcom_taobao_remotebunle.so";
            PackageInfo info = activity.getPackageManager().getPackageArchiveInfo(path, 0);

            try {
                if (Framework.getBundle(info.packageName) != null){
                    System.out.println("uninstallBundle.");
                    Atlas.getInstance().uninstallBundle(info.packageName);
                }

                Atlas.getInstance().installBundle(info.packageName, new File(path));

            } catch (BundleException e) {
                System.out.println("installBundle e = " + e);
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // end

            Intent intent = new Intent();
            intent.setClassName(activity, "com.taobao.remotebunle.RemoteBundleActivity");
            startActivity(intent);
        });


    }

}
