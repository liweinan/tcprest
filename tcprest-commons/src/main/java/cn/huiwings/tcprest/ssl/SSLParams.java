package cn.huiwings.tcprest.ssl;

/**
 * @author Weinan Li
 * @created_at 08 25 2012
 */
public class SSLParams {
    private String keyStorePath;
    private String keyStoreStorePass;
    private String keyStoreKeyPass;

    private String trustStorePath;
    private String trustStoreStorePass;
    private String trustStoreKeyPass;

    private boolean needClientAuth = false;

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public String getKeyStoreStorePass() {
        return keyStoreStorePass;
    }

    public void setKeyStoreStorePass(String keyStoreStorePass) {
        this.keyStoreStorePass = keyStoreStorePass;
    }

    public String getKeyStoreKeyPass() {
        return keyStoreKeyPass;
    }

    public void setKeyStoreKeyPass(String keyStoreKeyPass) {
        this.keyStoreKeyPass = keyStoreKeyPass;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public String getTrustStoreStorePass() {
        return trustStoreStorePass;
    }

    public void setTrustStoreStorePass(String trustStoreStorePass) {
        this.trustStoreStorePass = trustStoreStorePass;
    }

    public String getTrustStoreKeyPass() {
        return trustStoreKeyPass;
    }

    public void setTrustStoreKeyPass(String trustStoreKeyPass) {
        this.trustStoreKeyPass = trustStoreKeyPass;
    }

    public boolean isNeedClientAuth() {
        return needClientAuth;
    }

    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }
}
