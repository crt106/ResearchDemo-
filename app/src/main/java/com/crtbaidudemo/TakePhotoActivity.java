package com.crtbaidudemo;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.Poi;

import java.io.File;
import java.util.List;

public class TakePhotoActivity extends AppCompatActivity
{
    //region 字段和常量
    public final int TAKE_PHOTO=1;
    public int CurrentAPILevel=0;
    public boolean IsFirstCreat;//判断是不是创建该Activity
    //endregion
    //region 控件
    public ImageView photoview;//展示照相的图片滴ImageView
    public TextView PositionInfo;
    public ProgressDialog pd;
    //endregion
    //region 百度地图信息和组件
    public BDLocation ThistimeLocationInfo;//存储本次照片的位置信息
    public String ChoosedPOI;//当前选择的周边地点的名字
    public LocationClient mLocationClient = null;
    private BDAbstractLocationListener myListener=new BDAbstractLocationListener()
    {
        //获取位置信息回调方法
        @Override
        //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
        public void onReceiveLocation(BDLocation location)
        {
            ThistimeLocationInfo=location;
            //构建字符串
            StringBuffer sb=new StringBuffer();

            //以下只列举部分获取经纬度相关（常用）的结果信息
            //更多结果信息获取说明，请参照类参考中BDLocation类中的说明
            sb.append("定位时间");
            sb.append(location.getTime());

            sb.append("\n定位类型 : ");
            sb.append(location.getLocType());

            sb.append("\n坐标系类型");
            sb.append(location.getCoorType());

            sb.append("\n纬度 : ");
            sb.append(location.getLatitude());

            sb.append("\n经度 : ");
            sb.append(location.getLongitude());

            sb.append("\n国家 : ");
            sb.append(location.getCountry());

            sb.append("\n城市 : ");
            sb.append(location.getCity());

            sb.append("\n区 : ");
            sb.append(location.getDistrict());

            sb.append("\n街道 : ");
            sb.append(location.getStreet());

            sb.append("\naddr : ");
            sb.append(location.getAddrStr());

            sb.append("\n所处室内还是室外: ");// *****返回用户室内外判断结果*****
            if (location.getUserIndoorState()==1)
            {
                sb.append("室内");
            }
            else
            {
                sb.append("室外");
            }

            sb.append("\n位置语义化信息: ");
            sb.append(location.getLocationDescribe());// 位置语义化信息
            //sb.append("\nPoi: ");

            PositionInfo.setText(sb.toString());
        }
    };
    //endregion
    //region 照片
    private File ImageFile;//照片文件
    private Uri fileUri = Uri.EMPTY;//用于拍照的文件Uri
    //endregion
    //region 服务
    //与Service的连接
    private DemoClientService.ClientServiceBinder mBinder;
    private ServiceConnection serviceConnection=new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            mBinder=(DemoClientService.ClientServiceBinder)service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {

        }
    };
    //endregion
    //region 生命周期和按钮响应
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        IsFirstCreat=true;
        //声明LocationClient类
        mLocationClient = new LocationClient(getApplicationContext());
        //注册监听函数
        mLocationClient.registerLocationListener(myListener);
        //绑定相应控件
        photoview=(ImageView) findViewById(R.id.imageViewPhoto);
        PositionInfo=(TextView) findViewById(R.id.textViewPositionInfo);

        pd=new ProgressDialog(TakePhotoActivity.this);
        pd.setTitle("正在上传...");
        pd.setMessage("这服务器上行比下行快诶...不急不急");
        pd.setCancelable(false);
        //获取当前API
        CurrentAPILevel= Build.VERSION.SDK_INT;
        //绑定Service
        Intent serviceIntent=new Intent(TakePhotoActivity.this,DemoClientService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onPause()
    {
        super.onPause();
        IsFirstCreat=false;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if(IsFirstCreat)
            UseCamera();
    }

    //重新拍照按钮点下
    public void onRetakePhotoButtonClick(View v)
    {
        UseCamera();
    }
    //选择POI按钮点下
    public void onChoosePOIButtonClick(View v)
    {
        ChoosePOI();
    }
    //上传照片按钮点下
    public void onUploadButtonClick(View v)
    {
        UploadPhoto();
    }
//endregion

    //StartActivity操作返回
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            //照相返回
            case(TAKE_PHOTO):
            {
                try
                {
                    Bitmap tempBitmap= MediaStore.Images.Media.getBitmap(getContentResolver(),fileUri);
                    photoview.setImageBitmap(tempBitmap);
                    //获取位置信息
                    StartGetLocationInfo();
                }
                catch (Exception e)
                {
                    Log.e("错误",e.toString() );
                }
                break;
            }
        }
    }

    //开启照相机拍照的方法
    public void UseCamera()
    {
        Intent openCameraIntent = new Intent();
        ImageFile = new File(getExternalCacheDir(), "temp.jpg");
        // 如果存在就删了重新创建
        try
        {
            if (ImageFile.exists())
            {
                ImageFile.delete();
            }
            ImageFile.createNewFile();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        //照相机的Intent
        openCameraIntent = new Intent("android.media.action.IMAGE_CAPTURE");
        if (CurrentAPILevel <= 24)//安卓7.0之前的操作
        {
            fileUri = Uri.fromFile(ImageFile);
        }
        else
        {
            //兼容android7.0 使用共享文件的形式
            fileUri= FileProvider.getUriForFile(getApplicationContext(),getPackageName(),ImageFile);
        }

        openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        //开启照相机
        startActivityForResult(openCameraIntent, TAKE_PHOTO);
    }

    //开始获取位置信息
    public void StartGetLocationInfo()
    {
        mLocationClient.setLocOption(SettingActivity.AppLocationSetting.getOption());

        if(!mLocationClient.isStarted())
            mLocationClient.start();
        else
        {
            mLocationClient.stop();
            mLocationClient.restart();
        }
    }

    //选择POI
    public void ChoosePOI()
    {
        ChoosedPOI=null;
        Log.w("提示","开始选择POI" );
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
        //设置对话框内容和按下回调
        builder.setItems(PoiArray, (DialogInterface,i)->
            {
                //戳你母娘的java匿名类传参
               ChoosedPOI=PoiArrayfinal[i];
               //只有MIUI的Toast会自带应用名称 真尼玛好笑哈哈哈哈哈哈嗝
               Toast.makeText(getApplicationContext(),("选择的地点是["+ChoosedPOI+"]"), Toast.LENGTH_LONG).show();
            }
        );
        builder.create().show();
    }

    //上传照骗
    public void UploadPhoto()
    {
        if(ChoosedPOI==null||ChoosedPOI.isEmpty())
        {
            Toast.makeText(getApplicationContext(), "请选择地点!", Toast.LENGTH_LONG).show();
            return;
        }
        PhotoTest tempphoto=new PhotoTest(ThistimeLocationInfo,"01",ChoosedPOI,ImageFile,ImageFile.length());
        OnTaskFinishiedListener taskFinishiedListener=new OnTaskFinishiedListener()
        {
            @Override
            public void OnTaskSucceed(Object result)
            {
                Toast.makeText(getApplicationContext(), "上传完成", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void OnTaskFailed(Object info)
            {
                Toast.makeText(getApplicationContext(), "上传失败！"+info, Toast.LENGTH_SHORT).show();
            }
        };
        //执行任务
        mBinder.UploadPhoto(tempphoto, pd,taskFinishiedListener );
    }


}
