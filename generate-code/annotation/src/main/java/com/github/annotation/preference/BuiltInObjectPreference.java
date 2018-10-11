package com.github.annotation.preference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 目前 default 值只能是字面量, 或者计算式, 不能是其他
 * 用于注解内置对象
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface BuiltInObjectPreference {
  String prefFile() default "";
  
  String key() default "";
  
  String prefixKey() default "";
  
  boolean removable() default false;
}
