package com.example.entity;

import org.springframework.context.annotation.Bean;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Consumer;


public interface BaseData {

    default <V> V asViewObject(Class<V> clazz, Consumer<V> consumer) {//其他额外值的赋予，如token，通过consumer给出
        V v = asViewObject(clazz);
        consumer.accept(v);
        return v;
    }

    default <V> V asViewObject(Class<V> clazz) {
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

    private void convert(Field field, Object vo) {
        try{
            Field source = this.getClass().getDeclaredField(field.getName());//根据传入字段先拿到当前对象成员变量
            field.setAccessible(true);
            source.setAccessible(true);
            field.set(vo,source.get(this));//根据字段表field，直接给vo对象赋值
        } catch ( IllegalAccessException | NoSuchFieldException ignored) {
            //如果存在查询对象有,当前对象没有的成员变量，直接忽略
        }

    }
}
