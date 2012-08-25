package io.tcprest.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author Weinan Li
 * @created_at 08 25 2012
 */
public class TimeoutAnnotationHandler extends AnnotationHandler {

    public static int getTimeout(Method mtd) {

        OperationCallback<Integer> op = new OperationCallback<Integer>() {
            public Integer process(Annotation annotation) {
                return ((Timeout) annotation).second();
            }
        };

        Integer timeout = scanAndProcessAnnotation(
                mtd.getAnnotations(), Timeout.class, op);

        if (timeout == null) {
            return 0;
        } else {
            return timeout;
        }
    }
}
