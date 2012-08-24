package io.tcprest.annotations;

import io.tcprest.logger.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author Weinan Li
 * @created_at 08 21 2012
 */
public class ParamHandler {

	public static int getTimeout(Method mtd) {

		OperationCallback<Integer> op = new OperationCallback<Integer>() {
			public Integer process(Annotation annotation) {
				return ((Timeout) annotation).second();
			}
		};

		Integer timeout = (Integer) scanAndProcessAnnotation(
				mtd.getAnnotations(), Timeout.class, op);

		if (timeout == null) {
			return 0;
		} else {
			return timeout;
		}
	}

	public static boolean isSSLEnabled(Class clazz) {
		OperationCallback<Boolean> op = new OperationCallback<Boolean>() {
			public Boolean process(Annotation annotation) {
				return true;
			}
		};

		Boolean sslEnabled = (Boolean) scanAndProcessAnnotation(
				clazz.getAnnotations(), SSLEnabled.class, op);

		if (sslEnabled != null) {
			return true;
		} else {
			for (Class _clazz : clazz.getInterfaces()) {
				sslEnabled = (Boolean) scanAndProcessAnnotation(
						_clazz.getAnnotations(), SSLEnabled.class, op);
				if (sslEnabled != null)
					return true;
			}

		}

		return false;
	}

	private static Object scanAndProcessAnnotation(Annotation[] annotations,
			Class targetAnnotation, OperationCallback op) {
		for (Annotation atn : annotations) {
			if (atn.annotationType().equals(targetAnnotation)) {
				return op.process(atn);
			}
		}
		return null;
	}

	private interface OperationCallback<T> {
		public T process(Annotation annotation);
	}
}
