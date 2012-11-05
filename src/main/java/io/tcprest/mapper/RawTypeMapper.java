package io.tcprest.mapper;

import io.tcprest.commons.Base64;

import java.io.*;

/**
 * @author Weinan Li
 * @created_at 08 20 2012
 */
public class RawTypeMapper implements Mapper {
    public Object stringToObject(String param) {
        try {
            ByteArrayInputStream source = new ByteArrayInputStream(Base64.decode(param));
            ObjectInputStream is = new ObjectInputStream(source);
            return is.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String objectToString(Object object) {
        try {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(target);
            os.writeObject(object);
            os.flush();
            os.close();
            return Base64.encode(target.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
