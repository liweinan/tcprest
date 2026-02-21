package cn.huiwings.tcprest.mapper;

/**
 * @author Weinan Li
 * @created_at 08 20 2012
 */
public class ExceptionMapper implements Mapper {
    @Override
    public Object stringToObject(String param) {
        return new Exception(param);
    }

    @Override
    public String objectToString(Object object) {
        return ((Exception) object).getMessage();
    }
}
