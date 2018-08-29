package com.crtbaidudemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
{
    //百度地图API RequestCode
    public final int BAIDU_PREMISSION_STATE=1;

    //region 服务
    //服务连接匿名类
    private ServiceConnection serviceConnection=new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            serviceBinder=(DemoClientService.ClientServiceBinder) service;
            serviceBinder.CheckandConnectServer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {

        }
    };
    //服务Binder
    private DemoClientService.ClientServiceBinder serviceBinder;
    //服务Intent
    Intent ServiceIntent;
    //endregion


    //region 生命周期
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPersimmions();
        ServiceIntent=new Intent(MainActivity.this,DemoClientService.class);
        bindService(ServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        stopService(ServiceIntent);

    }
    //endregion

    //获取权限
    @TargetApi(23)
    private void getPersimmions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            ArrayList<String> permissions = new ArrayList<String>();
            /***
             * 定位权限和照相为必须权限，用户如果禁止，则每次进入都会申请
             */
            // 定位精确位置
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            //照相机权限
            if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
            {
                permissions.add(Manifest.permission.CAMERA);
            }
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
            {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }


            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), BAIDU_PREMISSION_STATE);
            }
        }
    }
    //获取权限回调函数
    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean IsGetPermission = true;
        switch (requestCode)
        {
            case (BAIDU_PREMISSION_STATE):
            {
                for (int results : grantResults)
                {
                    if (results == PackageManager.PERMISSION_GRANTED)
                        continue;
                    IsGetPermission = false;
                }
                break;
            }
        }
        if (IsGetPermission)
        {
            Toast.makeText(getApplicationContext(), "权限获取成功", Toast.LENGTH_LONG).show();
        }
        else
        {
            Toast.makeText(getApplicationContext(), "权限获取失败！", Toast.LENGTH_LONG).show();
        }
    }


    //region 相应按钮事件
    public void onTakePhotoButton_Click(View v)
    {
        Intent intent=new Intent(MainActivity.this,TakePhotoActivity.class);
        startActivity(intent);
    }
    public void onSettingButton_Click(View v)
    {
        Intent intent=new Intent(MainActivity.this,SettingActivity.class);
        startActivity(intent);
    }
    public void onGetPhotoButton_Click(View v)
    {
        Intent intent=new Intent(MainActivity.this,GetPhotoActivity.class);
        startActivity(intent);
    }
    //endregion


}
