package com.crtbaidudemo;

import com.baidu.location.BDLocation;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.baidu.mapapi.utils.DistanceUtil;

import java.io.File;
import java.util.List;

//实验用照片类
public class PhotoTest
{
    //图片全部位置信息
    private BDLocation BDLocationInfo;

    public BDLocation getBDLocationInfo()
    {
        return BDLocationInfo;
    }

    public String Photoname;
    public String POIname;
    public File PhotoFile; //直接是以文件来存储吗？？
    public long fileSize;
    public String Description;//描述

    //region 判断最优点时用到的变量
    String CoorType;
    double latitude;
    double longtitude;

    //构造函数
    public PhotoTest(BDLocation location, String photoname, String POIname, File photo, long fileSize)
    {
        BDLocationInfo = location;
        Photoname = photoname;
        this.POIname = POIname;
        PhotoFile = photo;
    }

    //缺省构造函数
    public PhotoTest(String POIname, String CoorType, double latitude, double longtitude,long fileSize)
    {
        this.POIname = POIname;
        this.CoorType = CoorType;
        this.latitude = latitude;
        this.longtitude = longtitude;
        this.fileSize=fileSize;
    }

    //判断最优点的序号
    public static int GetBestPoint(List<PhotoTest> list, double yourla, double yourlo, String coorType)
    {
        //用户所在位置
        LatLng userlat = new LatLng(yourla, yourlo);
        CoordinateConverter converter = new CoordinateConverter();
        if (!coorType.equals("bd09ll"))
        {
            converter.from(CoordinateConverter.CoordType.valueOf(coorType));
            userlat = converter.coord(userlat).convert();
        }

        double minDis=999999999;
        int BestPointID=0;
        //对链表中坐标进行转换和距离运算
        for (int i = 0; i < list.size(); i++)
        {
            PhotoTest temp = list.get(i);
            LatLng thislat = new LatLng(temp.latitude, temp.longtitude);
            if (!coorType.equals("bd09ll"))
                //进行坐标转换
                thislat=converter.coord(thislat).convert();
            //进行距离运算
            double tempdis=DistanceUtil.getDistance(thislat, userlat);
            if(tempdis<minDis)
            {
                minDis=tempdis;
                BestPointID=i;
            }
        }
        return BestPointID;
    }

}


