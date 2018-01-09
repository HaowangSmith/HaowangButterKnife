package com.base.inject;

import android.app.Activity;

/**
 * Created by Administrator on 2017/3/1 0001.
 */

public class InjectView {
    public  static  void bind(Activity activity)
    {
        String clsName=activity.getClass().getName();//返射拿到文件名
        try {
            Class<?> viewBidClass= Class.forName(clsName+"$$ViewBinder");//返射拿到类
            ViewBinder viewBinder= (ViewBinder) viewBidClass.newInstance();//返射后类型
            viewBinder.bind(activity);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }
}
