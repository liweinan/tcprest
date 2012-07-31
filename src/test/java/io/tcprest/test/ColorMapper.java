package io.tcprest.test;

import io.tcprest.mapper.Mapper;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class ColorMapper implements Mapper {
    public Object stringToObject(String param) {
        return new Color(param);
    }

    public String objectToString(Object object) {
        if (object instanceof Color) {
            return ((Color) object).getName();
        } else {
            return null;
        }

    }
}
