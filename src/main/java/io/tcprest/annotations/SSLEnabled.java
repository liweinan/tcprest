package io.tcprest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SSLEnabled {
	boolean needClientAuth();
	
	String serverKeyStore();
	String serverKeyStorePassword();
	String serverTrustStore();
	String serverTrustStorePassword();
	
	String clientKeyStore();
	String clientKeyStorePassword();
	String clientTrustStore();
	String clientTrustStorePassword();
	
}
