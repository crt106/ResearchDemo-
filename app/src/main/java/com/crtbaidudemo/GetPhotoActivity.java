package com.crtbaidudemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.Poi;
import com.baidu.mapapi.SDKInitializer;

import java.io.File;
import java.util.List;

public class GetPhotoActivity extends AppCompatActivity
{

    //region 控件
    TextView ShortLocationInfo; //位置语义显示
    ImageView imageView;
    ProgressDialog pd; //进度对话框
    //AlertDialog alertDialog;
    //endregion
    //region 百度地图组件
    public BDLocation ThistimeLocationInfo;//存储本次照片的位置信息
    public String ChoosedPOI;//当前选择的周边地点的名字
    public LocationClient mLocationClient = null;
    private BDAbstractLocationListener mListenner=new BDAbstractLocationListener()
    {
        @Override
        public void onReceiveLocation(BDLocation bdLocation)
        {
            ThistimeLocationInfo=bdLocation;
            ShortLocationInfo.setText(bdLocation.getLocationDescribe());
        }
    };
    //endregion
    //region 服务
    private ServiceConnection serviceConnection=new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            mbinder=(DemoClientService.ClientServiceBinder)service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {

        }
    };
    private DemoClientService.ClientServiceBinder mbinder;
    //endregion
    //region 照片
    private File ImageFile;
    private Uri ImageUri;
    //endregion

    //region 生命周期和按钮相应
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_photo);
        //初始化百度地图API的某些组件
        SDKInitializer.initialize(getApplicationContext());
        //绑定百度地图组件
        mLocationClient=new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(mListenner);
        mLocationClient.setLocOption(SettingActivity.AppLocationSetting.getOption()); //设置属性
        //*****启动Activity的时候就进行一次定位******
        if(mLocationClient.isStarted())
            mLocationClient.restart();
        else
            mLocationClient.start();
        //绑定服务
        Intent serviceintent=new Intent(GetPhotoActivity.this,DemoClientService.class);
        bindService(serviceintent, serviceConnection, BIND_AUTO_CREATE);
        //获取相关组件
        ShortLocationInfo=(TextView)findViewById(R.id.textViewShortLocation);
        imageView=(ImageView)findViewById(R.id.ViewShowDownload);
        pd=new ProgressDialog(GetPhotoActivity.this);
        pd.setTitle("正在下载图片 服务器小水管啦");
        pd.setMessage("不要慌不要慌");
        pd.setCancelable(false);


    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    public void onCandDButton_Click(View v)
    {
        ChoosePOI();
    }

    public void onDownloadButton_Click(View v)
    {
        StartDownloadPhoto();
    }
    //endregion

    //选择POI
    public void ChoosePOI()
    {
        ChoosedPOI=null;
        Log.w(Activity.class.getName()+",提示","开始选择POI" );
        //获取本次定位信息中的Poi列表
        List<Poi> tempPoiList=ThistimeLocationInfo.getPoiList();
        String[] PoiArray;
        //判断非空
        if(tempPoiList==null || tempPoiList.isEmpty())
        {
            return;
        }
        //构建数组和它的final中转量
        PoiArray=new String[tempPoiList.size()];
        for(int i=0;i<tempPoiList.size();i++)
        {
            PoiArray[i]=tempPoiList.get(i).getName();
        }
        final String[] PoiArrayfinal=PoiArray;// final中转变量 匿名类里面只能访问final变量

        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("选择照片对应的地点");
        builder.setCancelable(false);
        builder.setItems(PoiArray, (DialogInterface DialogInterface, int i) ->
                {
                    //戳你母娘的java匿名类传参
                    ChoosedPOI=PoiArrayfinal[i];
                    //只有MIUI的Toast会自带应用名称 真尼玛好笑哈哈哈哈哈哈嗝
                    Toast.makeText(getApplicationContext(),("选择的地点是["+ChoosedPOI+"]"), Toast.LENGTH_LONG).show();
                }
        );
       builder.create().show();
    }

    //下载照骗啦
    public void StartDownloadPhoto()
    {
        if(ChoosedPOI==null||ChoosedPOI=="")
        {
            Toast.makeText(GetPhotoActivity.this,"请先选择POI~" , Toast.LENGTH_SHORT);
            return;
        }
        Log.e("提示", "开始下载照片");
        OnTaskFinishiedListener taskFinishiedListener=new OnTaskFinishiedListener()
        {
            @Override
            public void OnTaskSucceed(Object result)
            {
                //显示照片
                ImageFile=(File)result;
                Bitmap b=BitmapFactory.decodeFile(ImageFile.getAbsolutePath());
                imageView.setImageBitmap(b);
                Toast.makeText(getApplicationContext(),"下载成功" , Toast.LENGTH_SHORT).show();
            }

            @Override
            public void OnTaskFailed(Object info)
            {
                Toast.makeText(getApplicationContext(),"下载失败！" +info, Toast.LENGTH_SHORT).show();
                return;
            }
        };
        mbinder.DownLoadPhoto(ChoosedPOI,ThistimeLocationInfo ,pd ,taskFinishiedListener);
    }
}
