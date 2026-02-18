package cn.huiwings.tcprest.annotations;

import java.lang.annotation.Annotation;

/**
 * @author Weinan Li
 * @created_at 08 21 2012
 */
public class AnnotationHandler {

    protected static <T> T scanAndProcessAnnotation(Annotation[] annotations,
                                                     Class targetAnnotation, OperationCallback<T> op) {
        for (Annotation atn : annotations) {
            if (atn.annotationType().equals(targetAnnotation)) {
                return op.process(atn);
            }
        }
        return null;
    }

    protected interface OperationCallback<T> {
        public T process(Annotation annotation);
    }
}
