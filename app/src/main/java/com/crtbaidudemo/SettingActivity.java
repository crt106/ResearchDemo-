package com.crtbaidudemo;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.baidu.location.LocationClientOption;

public class SettingActivity extends AppCompatActivity
{
    //静态设置类 供其他类直接调用
    public static BDLocationSetting AppLocationSetting = new BDLocationSetting();
    private RadioGroup LocationModeGroup;
    private RadioGroup CoorTypeGroup;

    //region 生命周期
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        //绑定相应控件
        LocationModeGroup = (RadioGroup) findViewById(R.id.RadioGroupLocationMode);
        CoorTypeGroup = (RadioGroup) findViewById(R.id.RadioGroupCoor);

        //为RadioGroup添加监听事件以及做出相应的处理
        LocationModeGroup.setOnCheckedChangeListener((RadioGroup, id) ->
                {
                    String RadioButtonText = ((RadioButton) findViewById(id)).getText().toString();
                    AppLocationSetting.setLocationMode(RadioButtonText);
                    Log.i("定位模式改变:", RadioButtonText);
                }
        );

        CoorTypeGroup.setOnCheckedChangeListener((radioGroup, i) ->
                {
                    String coorType = "";
                    switch (i)
                    {
                        case (R.id.radioButton_bd09):
                        {
                            coorType = "bd09";
                            break;
                        }
                        case (R.id.radioButton_bd09ll):
                        {
                            coorType = "bd09ll";
                            break;
                        }
                        case (R.id.radioButton_gcj02):
                        {
                            coorType = "gcj02";
                            break;
                        }
                    }
                    AppLocationSetting.setCoorType(coorType);
                }
        );
    }
    //endregion


}

class BDLocationSetting
{
    //定位设定类
    private LocationClientOption option = new LocationClientOption();

    //java里面没有属性是有点小难受哦
    public LocationClientOption getOption()
    {
        return option;
    }

    public BDLocationSetting()
    {
        option.setCoorType("bd09ll");
        option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving);
        option.setScanSpan(0);
        option.setIsNeedAddress(true);
        option.setIsNeedLocationDescribe(true);
        option.setIsNeedLocationPoiList(true);
    }

    public void setCoorType(String s)
    {
        //检测设置的合理性
        if (s == "bd09" || s == "bd09ll" || s == "gcj02")
            option.setCoorType(s);
        else
        {
            Log.e("选择坐标类型", "出错:" + s + "字符串不符合要求");
            return;
        }

    }

    public void setLocationMode(String s)
    {
        switch (s)
        {

            case ("高精度模式"):
                option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
                break;

            case ("低功耗模式"):
                option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving);
                break;

            case ("仅设备"):
                option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
                break;
            default:
                Log.e("选择定位模式", "出错:" + s + "字符串不符合要求");
                break;
        }
    }

}
