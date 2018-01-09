package com.base.inject_annotion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Administrator on 2017/3/1 0001.
 */
@Target(ElementType.FIELD)//可以给属性进行注解
@Retention(RetentionPolicy.CLASS)//代表运行在编译时
public @interface BindView {
    int value();
}
