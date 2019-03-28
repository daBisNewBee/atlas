package com.taobao.remotebunle;

import android.os.Bundle;
import android.taobao.atlas.remote.IRemote;

public class MyRemoteTransact implements IRemote {
    @Override
    public Bundle call(String s, Bundle bundle, IResponse iResponse) {
        System.out.println("MyRemoteTransact.call" + s);
        System.out.println("bundle = " + bundle);
        bundle.putString("apple-key", "apple-value");
        return bundle;
    }

    @Override
    public <T> T getRemoteInterface(Class<T> aClass, Bundle bundle) {
        return null;
    }
}
