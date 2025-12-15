package com.example.entity;



import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 用于DTO快速转换VO实现，只需将DTO类继承此类即可使用
 */
public interface BaseData {

    /**
     * 创建指定的VO类并将当前DTO对象中的所有成员变量值直接复制到VO对象中
     * @param clazz 指定VO类型
     * @param consumer 返回VO对象之前可以使用Lambda进行额外处理
     * @return 指定VO对象
     * @param <V> 指定VO类型
     */
    default <V> V asViewObject(Class<V> clazz, Consumer<V> consumer) {//其他额外值的赋予，如token，通过consumer给出
        V v = asViewObject(clazz);
        consumer.accept(v);
        return v;
    }
    /**
     * 创建指定的VO类并将当前DTO对象中的所有成员变量值直接复制到VO对象中
     * @param clazz 指定VO类型
     * @return 指定VO对象
     * @param <V> 指定VO类型
     */
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
    /**
     * 内部使用，快速将当前类中目标对象字段同名字段的值复制到目标对象字段上
     * @param field 目标对象字段
     * @param vo 目标对象
     */
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
