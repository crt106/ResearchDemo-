package com.crtbaidudemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.text.BoringLayout;
import android.util.Log;
import android.widget.Toast;

import com.baidu.location.BDLocation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DemoClientService extends Service
{

    //region 套接字和IP地址
    public Socket ClientSocket=new Socket();
    //套接字输出流和输入流
    public OutputStream  Clientop;
    public InputStream   Clientin;
    String ServerIP1="120.79.7.230";//阿里云服务器ip
    String ServerIP2="10.133.1.126";//寝室crt的ip
    String ServerIP3="10.135.97.4";//图书馆crt的ip
    String ServerIP4="192.168.43.94";//热点的crt的ip
    String LocalServerIP="127.0.0.1";
    final String[] ServerIpList={ServerIP1,ServerIP2,ServerIP3,ServerIP4,LocalServerIP};   //服务器IP列表

    //endregion

    public ClientServiceBinder mBinder=new ClientServiceBinder();
    //服务提供类
    class ClientServiceBinder extends Binder
    {

        //检查连接
        public Boolean CheckandConnectServer()
        {
            return DemoClientService.this.CheckandConnectServer();
        }
        //上传图片
        public void UploadPhoto(PhotoTest photoTest,ProgressDialog pd,OnTaskFinishiedListener l)
        {
            DemoClientService.this.Upload(photoTest,pd,l);
        }

        //下载图片
        public void DownLoadPhoto(String POIname,BDLocation locationInfo,ProgressDialog pd,OnTaskFinishiedListener l)
        {
            DemoClientService.this.Download(POIname,locationInfo,pd,l);
        }
    }
    //重写与活动绑定
    @Override
    public IBinder onBind(Intent intent)
    {
        Log.e("Service", intent.getComponent().getClassName()+"在请求绑定服务");
        return mBinder;
    }
    //region Service生命周期
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.e("Net", "服务已启动");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        try
        {
            ClientSocket.close();
            Log.e("Net", "服务终止");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return super.onStartCommand(intent, flags, startId);
    }
    //endregion

    //检查连接 如果断开连接 则自动连接回合适的服务器
    public boolean CheckandConnectServer()
    {
        class ConnectTask extends AsyncTask<Void,Boolean,Boolean>
        {
            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... voids)
            {
                try
                {
                    //绑定本地套接字并且连接
                    ClientSocket=new Socket(ServerIP1,7777);
                    Clientop=ClientSocket.getOutputStream();
                    Clientin=ClientSocket.getInputStream();
                }
                catch (IOException e)
                {
                    Log.w("Net","连接到服务器失败" );
                    Log.e("Net",e.toString() );
                    return false;
                }
                if(ClientSocket.isConnected())
                {
                    Log.w("Net","连接到服务器成功" );
                    return true;
                }
                else
                {
                    Log.w("Net","连接到服务器失败" );
                    return false;
                }

            }

        }
        try
        {
            boolean Isconnect=new ConnectTask().execute().get();
            return Isconnect;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }


    }

    //上传照片的方法 这里传入的pd是用于显示进度咯
    public void Upload(PhotoTest p,ProgressDialog pd,OnTaskFinishiedListener l)
    {

        class UploadTask extends AsyncTask<PhotoTest,Double,Boolean>
        {

            //region 任务回调接口及其set方法
            OnTaskFinishiedListener onTaskFinishiedListener;
            public void setOnTaskFinishiedListener(OnTaskFinishiedListener l)
            {
                onTaskFinishiedListener=l;
            }
            String FailInfo="";//给任务失败时预留的消息
            //endregion

            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();
                pd.show();
            }

            @Override
            protected Boolean doInBackground(PhotoTest... photoTests)
            {
                //获取 传入参数
                PhotoTest p=photoTests[0];

                //获取各项字段
                String POIname=p.POIname;
                String CoorType=p.getBDLocationInfo().getCoorType();
                double latitude=p.getBDLocationInfo().getLatitude();
                double longtitude=p.getBDLocationInfo().getLongitude();
                File photo=p.PhotoFile;

                JSONObject jsondata=new JSONObject();

                try
                {
                    //清理流中的数据
                    Clientop.flush();
                    //读取照片有关的数据
                    InputStream PhotoInStream= new FileInputStream(photo);
                    //读取文件流的长度
                    long count=PhotoInStream.available();
                    //拼接头Json数据
                    jsondata.put("Action", "UpLoadPhoto");
                    jsondata.put("POIname", POIname);
                    jsondata.put("CoorType", CoorType);
                    jsondata.put("latitude", latitude);
                    jsondata.put("longtitude", longtitude);
                    jsondata.put("filelength", count);
                    String outdata=jsondata.toString();
                    byte[] databyte=outdata.getBytes("utf-8");
                    //发送头数据
                    Clientop.write(databyte);
                    Clientop.flush();
                    //接收服务器回信
                    byte[] resultbuff=new byte[25];
                    int resultcount=Clientin.read(resultbuff,0,resultbuff.length);
                    String result= new String(resultbuff,0,resultcount,"utf-8");
                    if(result.equals("Json OK"))
                    {
                        Log.w("Net", "头部发送成功 开始上传文件");
                        //Json发送成功 开始文件传输
                        byte[] buffer=new byte[2048*60];
                        int len;
                        long sendedlen=0;
                        while(-1!=(len=PhotoInStream.read(buffer)))
                        {
                            Clientop.write(buffer,0,len);
                            sendedlen += len;
                            //刷新进度
                            publishProgress(sendedlen * 1.0 / count);
                            //判断传送完毕跳出2
                            if(sendedlen>=count)
                                break;
                        }
                        Log.e("Net", "发送文件完成");
                    }
                    else
                    {
                        Log.e("Net","服务器未响应 发送文件错误" );
                        return false;
                    }

                    //发送完成 返回
                    PhotoInStream.close();
                    return true;

                }
                catch (Exception e)
                {
                    Log.e("Net",e.getMessage() );
                    return false;
                }

            }

            @Override
            protected void onProgressUpdate(Double... values)
            {
                super.onProgressUpdate(values);
                double prog=values[0];
                pd.setMessage("这服务器上行比下行快诶...不急不急"+String.format(":%f%%",prog*100));
                Log.e("Net", String.format("文件已经上传:%f",prog*100));
            }

            @Override
            protected void onPostExecute(Boolean aBoolean)
            {
                if(aBoolean)
                //回传任务成功
                    onTaskFinishiedListener.OnTaskSucceed(aBoolean);
                else
                    onTaskFinishiedListener.OnTaskFailed(FailInfo);
                pd.dismiss();

            }
        }

        //这是内部任务类之外的代码
        UploadTask task=new UploadTask();
        task.setOnTaskFinishiedListener(l);
        task.execute(p);

    }

    //下载照骗的方法 这里传入的pd是用于显示进度咯
    public void Download(String POIname, BDLocation bdinfo, ProgressDialog pd,OnTaskFinishiedListener listener)
    {
        //任务执行匿名类
        class DownloadTask extends AsyncTask<Void,Double,File>
        {
            //region 任务回调接口及其set方法
            OnTaskFinishiedListener onTaskFinishiedListener;
            public void setOnTaskFinishiedListener(OnTaskFinishiedListener l)
            {
                onTaskFinishiedListener=l;
            }
            String FailInfo="";//给任务失败时预留的消息
            //endregion
            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();
                Toast.makeText(getApplicationContext(),"开始下啦 不要催了~" ,Toast.LENGTH_SHORT ).show();
                pd.show();
            }

            @Override
            protected File doInBackground(Void... v)
            {

                JSONObject job=new JSONObject();
                JSONArray jarray=new JSONArray();//新建两个Json相关对象
                List<PhotoTest> tempPointList=new LinkedList<>();//新建临时点序列链表
                double Userlatitude=bdinfo.getLatitude();
                double Userlongtitude=bdinfo.getLongitude();
                //创建照片文件

                SimpleDateFormat format=new SimpleDateFormat("yyyy_mm_ss_HH_mm");
                Date nowdate=new Date(System.currentTimeMillis());
                String filename= POIname+format.format(nowdate)+".jpg";//拼接文件名
                File ImageFile=new File(getFilesDir(),filename);

                try
                {
                    Clientop.flush();

                    job.put("Action", "DownloadPhoto");
                    job.put("POIname", POIname);
                    job.put("CoorType", bdinfo.getCoorType());
                    byte[] Jsonbyte=job.toString().getBytes();
                    //发送json数据
                    Clientop.write(Jsonbyte);
                    //接收服务器响应
                    byte[] buff=new byte[2048*60];
                    int resultcount=Clientin.read(buff);
                    String ResultJson=new String(buff,0,resultcount,"utf-8");
                    //接受到数据后开始进行处理
                    jarray=new JSONArray(ResultJson);
                    //这里判断一下接收到的jarray是不是长度为0
                    if(jarray.length()==0)
                    {
                        //说明没有查询到相应的图片 直接返回
                        FailInfo="没有查找到该地点的照片~";
                        return null;
                    }
                    //添加临时点链表 给对应方法去判断最优
                    for (int i=0;i<jarray.length();i++)
                    {
                        JSONObject tempJob=jarray.getJSONObject(i);
                        double latitude=tempJob.getDouble("latitude");
                        double longtitude=tempJob.getDouble("longtitude");
                        long filesize=tempJob.getLong("filesize");
                        tempPointList.add(new PhotoTest(POIname,bdinfo.getCoorType(),latitude,longtitude,filesize));
                    }
                    //获取最佳点ID
                    int bestId=PhotoTest.GetBestPoint(tempPointList, Userlatitude, Userlongtitude, bdinfo.getCoorType());
                    //向服务器发送最佳点ID 然后开始接收文件
                    Clientop.write(Integer.toString(bestId).getBytes());
                    long totalLength=tempPointList.get(bestId).fileSize,recvlen=0;
                    int len=0;
                    //创建文件流
                    FileOutputStream fos=new FileOutputStream(ImageFile);
                    while ((len=Clientin.read(buff))!=-1)
                    {
                        fos.write(buff,0,len);
                        recvlen+=len;
                        //刷新进度
                        publishProgress(recvlen*100.0/totalLength);
                       // Log.e("Net", String.format("正在写入文件%f%%",recvlen*100.0/totalLength));
                        if(recvlen>=totalLength)
                            break;
                    }
                    //文件读取完毕
                    fos.close();
                    Log.e("Net", String.format("文件写入完毕",recvlen*100.0/totalLength));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.e("Download", e.toString());
                }

                return ImageFile;
            }

            @Override
            protected void onProgressUpdate(Double... values)
            {
                super.onProgressUpdate(values);
                double progress=values[0];
                pd.setMessage("不要慌不要慌"+String.format("正在写入文件%f%%",progress));
                Log.e("Net", String.format("正在写入文件%f%%",progress));
            }

            @Override
            protected void onPostExecute(File file)
            {
                if(file!=null)
                //回传任务成功
                    onTaskFinishiedListener.OnTaskSucceed(file);
                else
                    onTaskFinishiedListener.OnTaskFailed(FailInfo);
                pd.dismiss();
            }
        }



        //这是Task类之外的代码
        DownloadTask task=new DownloadTask();
        task.setOnTaskFinishiedListener(listener);
        task.execute();
    }
}
