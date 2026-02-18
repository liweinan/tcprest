package cn.huiwings.tcprest.mapper;

import java.io.*;
import java.util.Base64;

/**
 * Mapper for automatically serializing/deserializing Serializable objects.
 *
 * <p>Uses Java's built-in object serialization and standard Base64 encoding.</p>
 *
 * @author Weinan Li
 * @created_at 08 20 2012
 */
public class RawTypeMapper implements Mapper {
    public Object stringToObject(String param) {
        try {
            ByteArrayInputStream source = new ByteArrayInputStream(Base64.getDecoder().decode(param));
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
            return Base64.getEncoder().encodeToString(target.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
