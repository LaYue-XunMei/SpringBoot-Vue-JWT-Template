package com.example.utils;

public class Const {
    //JWT令牌
    public static final String JWT_BLACK_LIST = "jwt:blackList";
    public final static String JWT_FREQUENCY = "jwt:frequency";//频率检测键前缀
    public static final int ORDER_CORS = -102;
    public static final int ORDER_LIMIT = -101;

    public static final String VERIFY_EMAIL_LIMIT = "verify:email:limit";
    public static final String VERITY_EMAIL_DATA = "verify:email:data";

    public static final String FLOW_LIMIT_COUNTER = "flow:counter";
    public static final String FLOW_LIMIT_BLOCK = "flow:block";

    //消息队列
    public final static String MQ_MAIL = "mail";

    //请求自定义属性
    public final static String ATTR_USER_ID = "userId";

    //默认用户角色
    public final static String ROLE_DEFAULT = "user";


}
