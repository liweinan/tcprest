package io.tcprest.test;

import java.io.Serializable;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class Color implements Serializable {
    private String name;

    public Color(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
