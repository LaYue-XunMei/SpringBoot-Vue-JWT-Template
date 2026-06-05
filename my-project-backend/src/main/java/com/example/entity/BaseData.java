package com.example.entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.Consumer;

public interface BaseData {

    default <V> V asViewObject(Class<V> clazz, Consumer<V> consumer){//加个函数式接口，其他额外值的赋予，如token，通过consumer给出
        V v = this.asViewObject(clazz);
        consumer.accept(v);
        return v;
    }

    default <V> V asViewObject(Class<V> clazz){//需要转换成的类型V
        try{
            Field[] declaredFields = clazz.getDeclaredFields();//先拿到所有的成员变量
            Constructor<V> constructor = clazz.getConstructor();//拿到构造器
            V v = constructor.newInstance();
            for (Field declaredField : declaredFields) {
                convert(declaredField,v);
            }
            return v;
        }catch (ReflectiveOperationException e){
            throw new RuntimeException(e.getMessage());
        }
    }

    private void convert(Field field,Object vo){//将当前dto的变量复制给vo对象
        try{
            Field source = this.getClass().getDeclaredField(field.getName());//获取同名字段
            field.setAccessible(true);//设置允许访问
            source.setAccessible(true);
            field.set(vo,source.get(this));//根据字段表field，直接给vo对象赋值
        }catch ( IllegalAccessException | NoSuchFieldException ignored) {
            //如果存在查询对象有,当前对象没有的成员变量，直接忽略
        }
    }
}
