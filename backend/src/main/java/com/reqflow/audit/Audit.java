package com.reqflow.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    String module();

    String action();

    String objectType() default "";

    /** 从同名方法参数中取业务对象 ID；为空时尝试取返回值 Result.data */
    String objectIdParam() default "";
}
