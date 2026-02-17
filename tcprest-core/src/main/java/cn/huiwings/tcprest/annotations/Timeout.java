package cn.huiwings.tcprest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Weinan Li
 * @created_at 08 21 2012
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Client
public @interface Timeout {
    int second();
}
