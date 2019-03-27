package com.taobao.firstbundle;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.taobao.atlas.framework.Framework;
import android.taobao.atlas.remote.IRemote;
import android.taobao.atlas.remote.IRemoteContext;
import android.taobao.atlas.remote.IRemoteTransactor;
import android.taobao.atlas.remote.RemoteFactory;
import android.taobao.atlas.remote.fragment.RemoteFragment;
import android.taobao.atlas.remote.transactor.RemoteTransactor;
import android.taobao.atlas.remote.view.RemoteView;
import android.taobao.atlas.runtime.RuntimeVariables;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.taobao.middleware.ICaculator;

/**
 *
 * 跨bundle(其实也是跨ClassLoader)调用组件的一般过程：
 * 1. 根据key获得所在的bundle名称（根据注册在manifest中的"meta-data"映射关系）
 * 2. 检查并安装对应bundle（若未安装）
 * 3. 创建组件并返回组件对象对应实例
 *   3.1  RemoteView：
 *        传回的"iRemoteContext"可以直接view使用，因为其本身就是一个framelayout，
 *        而远端view已经被add进去。
 *        注意："使用方不要通过findViewByID或者getChildAt等viewGroup的public
 *        方法尝试获取目标view并进行操作，RemoteView应该被当做View而不是ViewGroup"
 *   3.2  RemoteFragment：
 *        传回的"iRemote"可直接作为fragment使用
 *        TODO:"RemoteFragment的使用方拿到的仅仅是目标fragment的代理" 如何理解？
 *   3.3  RemoteTransactor：
 *        通过iRemote.getRemoteInterface获取远端ICaculator实例
 *        注意：ICaculator所在的"middlewarelibrary"
 *        只有在主"app"中才被compile，其他bundle中均是providedCompile
 *
 *  其中，
 *  1. 远端组件实例是由各自的"BundleClassLoader"加载的，BundleClassLoader如何获取？
 *  EmbeddedActivity.getClassLoader
 *  2. 所有与目标重用组件的通信，均以"Remote机制"进行
 *
 * 支持的组件包括：（载体组件）
 * 1. Fragment(RemoteFragment)
 * 2. View(RemoteView)
 * 3. func(RemoteTransactor)
 *
 * 需要共享的组件需要继承"IRemote"，
 * 为了方便将其实例缓存在载体组件的成员变量中：
 * 比如，
 *  MyRichFrameLayout、MyRichFragment、SecondBundleCaculator
 *
 */
public class UseremoteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_useremote);


        findViewById(R.id.btn_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoteFactory.requestRemote(RemoteView.class, UseremoteActivity.this, new Intent("atlas.view.intent.action.SECOND_RICH"),
                        new RemoteFactory.OnRemoteStateListener<RemoteView>() {
                            @Override
                            public void onRemotePrepared(RemoteView iRemoteContext) {
                                FrameLayout layout = (FrameLayout) findViewById(R.id.fl_content);
                                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                layout.addView(iRemoteContext,params);
                            }

                            @Override
                            public void onFailed(String s) {
                                Log.e("UserRemoteActivity",s);
                            }
                        });
            }
        });

        findViewById(R.id.btn_frag).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoteFactory.requestRemote(RemoteFragment.class, UseremoteActivity.this, new Intent("atlas.fragment.intent.action.SECOND_FRAGMENT"),
                        new RemoteFactory.OnRemoteStateListener<RemoteFragment>() {
                            @Override
                            public void onRemotePrepared(RemoteFragment iRemote) {
                                getSupportFragmentManager().beginTransaction().add(R.id.fl_content2,iRemote).commit();
                            }

                            @Override
                            public void onFailed(String s) {
                                Log.e("UserRemoteActivity",s);
                            }
                        });
            }
        });

        findViewById(R.id.btn_tran).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoteFactory.requestRemote(RemoteTransactor.class, UseremoteActivity.this, new Intent("atlas.transaction.intent.action.SECOND_TRANSACTION"),
                        new RemoteFactory.OnRemoteStateListener<RemoteTransactor>() {
                            @Override
                            public void onRemotePrepared(RemoteTransactor iRemote) {
                                // if you want remote call you
                                iRemote.registerHostTransactor(new IRemote() {
                                    @Override
                                    public Bundle call(String s, Bundle bundle, IResponse iResponse) {
                                        Toast.makeText(RuntimeVariables.androidApplication,"Command is "+s,Toast.LENGTH_SHORT).show();
                                        return null;
                                    }

                                    @Override
                                    public <T> T getRemoteInterface(Class<T> aClass, Bundle bundle) {
                                        return null;
                                    }
                                });

                                ICaculator caculator = iRemote.getRemoteInterface(ICaculator.class,null);
                                Toast.makeText(RuntimeVariables.androidApplication,"1+1 = "+caculator.sum(1,1),Toast.LENGTH_SHORT).show();

                                //you can also use this
//                                Bundle bundle = new Bundle();
//                                bundle.putInt("num1",1);
//                                bundle.putInt("num2",1);
//                                Bundle result  = iRemote.call("sum",bundle,null);
//                                Toast.makeText(RuntimeVariables.androidApplication,"1+1 = "+result.getInt("result"),Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailed(String s) {
                                Log.e("UserRemoteActivity",s);
                            }
                        });
            }
        });

    }
}
