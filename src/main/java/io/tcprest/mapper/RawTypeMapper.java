package io.tcprest.mapper;

import io.tcprest.commons.Base64;

import java.io.*;

/**
 * @author Weinan Li
 * @created_at 08 20 2012
 */
// TODO add doc for RawTypeMapper usage
// TODO Add support that no need mapper for Serizalizble object
public class RawTypeMapper implements Mapper {
    public Object stringToObject(String param) {
        try {
            ByteArrayInputStream source = new ByteArrayInputStream(Base64.decode(param));
            ObjectInputStream is = new ObjectInputStream(source);
            return is.readObject();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }
}
