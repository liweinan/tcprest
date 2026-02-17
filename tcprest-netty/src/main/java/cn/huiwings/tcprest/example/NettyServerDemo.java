package cn.huiwings.tcprest.example;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.NettyTcpRestServer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 11 05 2012
 *
 * @author <a href="mailto:l.weinan@gmail.com">Weinan Li</a>
 */
public class NettyServerDemo {

    public static interface RawType {
        public List getArrayList(List in);

        public HashMap<String, List<Color>> getComplexType(
                HashMap<String, List<Color>> in);
    }

    public static class RawTypeResource implements RawType {
        public List getArrayList(List in) {
            return in;
        }

        public HashMap<String, List<Color>> getComplexType(
                HashMap<String, List<Color>> in) {
            return in;
        }
    }


    public static class Color implements Serializable {
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

    public static void main(String[] args) throws Exception {
        Thread t = new Thread() {
            public void run() {
                NettyTcpRestServer server = new NettyTcpRestServer(8081);
                server.up();
                server.addResource(RawTypeResource.class);
            }
        };

        t.start();
        Thread.sleep(100);

        TcpRestClientFactory factory = new TcpRestClientFactory(RawType.class,
                "localhost", 8081);

        RawType client = (RawType) factory.getInstance();
        Color red = new Color("Red");

        {
            List request = new ArrayList();
            request.add(42);
            request.add(red);

            client.getArrayList(request);
        }

        {
            List<Color> list = new ArrayList<Color>();
            list.add(red);
            HashMap<String, List<Color>> request = new HashMap<String, List<Color>>();
            request.put("item", list);

            client.getComplexType(request);
        }

        t.join();
    }
}
