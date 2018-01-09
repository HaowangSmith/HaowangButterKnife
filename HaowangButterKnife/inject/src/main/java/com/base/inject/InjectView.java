package com.base.inject;

import android.app.Activity;

/**
 * Created by Administrator on 2017/3/1 0001.
 */

public class InjectView {
    public  static  void bind(Activity activity)
    {
        String clsName=activity.getClass().getName();//反射拿到使用方类名
        try {
            Class<?> viewBidClass= Class.forName(clsName+"$$ViewBinder");//反射拿到新生成的内部类
            ViewBinder viewBinder= (ViewBinder) viewBidClass.newInstance();//反射后类型
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
