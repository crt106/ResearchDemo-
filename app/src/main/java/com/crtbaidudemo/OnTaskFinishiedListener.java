package com.crtbaidudemo;
/**
 * 用于异步任务实现回调的接口
 */
public interface OnTaskFinishiedListener
{
    public void OnTaskSucceed(Object result);
    public void OnTaskFailed(Object info);
}
