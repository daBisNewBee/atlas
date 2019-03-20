package com.taobao.firstbundle;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.taobao.atlas.remote.IRemote;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FirstBundleFragment extends Fragment implements IRemote {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_blank, container, false);
        Log.v("bb", "this is add by dexpatch. second patch.");
        Log.v("bb", "create Sexy:" + new Sexy());

        Log.v("bb", "add by tpatch.");

        return rootView;
    }


//    @Override
//    public void onListFragmentInteraction(DummyContent.DummyItem item) {
//
//    }

    @Override
    public Bundle call(String s, Bundle bundle, IResponse iResponse) {
        return null;
    }

    @Override
    public <T> T getRemoteInterface(Class<T> aClass, Bundle bundle) {
        return null;
    }
}
