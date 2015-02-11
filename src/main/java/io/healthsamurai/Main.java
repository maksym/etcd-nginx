package io.healthsamurai;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.justinsb.etcd.EtcdClient;
import com.justinsb.etcd.EtcdNode;
import com.justinsb.etcd.EtcdResult;
import org.json.JSONObject;
import org.stringtemplate.v4.ST;

import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws Exception {
        EtcdClient client = new EtcdClient(URI.create("http://127.0.0.1:4001/"));
        String key = "/_coreos.com/fleet/state";

        EtcdResult state = client.get(key);
        for (EtcdNode node : state.node.nodes) {
            String machineID = new JSONObject(node.value).getJSONObject("machineState").getString("ID");
            String unitHash = new JSONObject(node.value).getString("unitHash");
            EtcdResult machine = client.get("/_coreos.com/fleet/machines/" + machineID);
            EtcdResult unit = client.get("/_coreos.com/fleet/unit/" + unitHash);
            String raw = new JSONObject(unit.node.value).getString("Raw");
            Pattern p = Pattern.compile("^Description.*\\[(.*)\\]", Pattern.MULTILINE);
            Matcher m = p.matcher(raw);
            if (m.find()) {
                String[] ports = m.group(1).split(",");
                for (String pair : ports) {
                    String server = node.key.substring(key.length() + 1).replaceAll(".service", "");
                    String ip = new JSONObject(machine.node.nodes.get(0).value).getString("PublicIP");
                    String listen = pair.split(":")[0];
                    String port = pair.split(":")[1];
                    //System.out.println(server);
                    //System.out.println(ip);
                    //System.out.println(listen);
                    //System.out.println(port);
                    URL url = Resources.getResource("template");
                    String template = Resources.toString(url, Charsets.UTF_8);
                    ST hello = new ST(template);
                    hello.add("server", server);
                    hello.add("ip", ip);
                    hello.add("listen", listen);
                    hello.add("port", port);
                    System.out.println(hello.render());
                }
            }
        }
    }
}
