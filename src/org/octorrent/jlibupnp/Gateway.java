package org.octorrent.jlibupnp;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Gateway {

    private InetAddress address;
    private URL controlUrl;
    private String serviceType;

    public Gateway(byte[] buf, int size, InetAddress address)throws IOException {
        this.address = address;
        String response = new String(buf, 0, size, StandardCharsets.UTF_8);
        String[] lines = response.split("\r\n");

        Map<String, String> headers = new HashMap<>();
        for(String line : lines){
            if(line.isEmpty()){
                break;
            }
            String[] parts = line.split(":", 2);
            if(parts.length == 2){
                headers.put(parts[0].trim(), parts[1].trim());
            }
        }

        String location = headers.getOrDefault("Location", headers.get("LOCATION"));
        if(location == null){
            throw new IOException("No location header found in response");
        }


        URL url = new URL(location);
        Socket socket = new Socket(url.getHost(), url.getPort());

        String request = "GET "+url.getPath()+" HTTP/1.1\r\n" +
                "Host: "+url.getHost()+"\r\n" +
                "Content-Type: text/xml\r\n\r\n";
        OutputStream out = socket.getOutputStream();
        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.flush();

        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder xmlContent = new StringBuilder();
        String line;
        boolean xmlStart = false;
        while((line = reader.readLine()) != null){
            if(line.isEmpty()){
                xmlStart = true;
                continue;
            }

            if(xmlStart){
                xmlContent.append(line).append("\n");
            }
        }
        socket.close();

        try{
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.toString().getBytes(StandardCharsets.UTF_8)));

            Node deviceNode = findNode(doc.getDocumentElement(), "device", "deviceList", "device", "deviceList", "device", "serviceList", "service");

            if(deviceNode != null && deviceNode instanceof Element){
                controlUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), ((Element) deviceNode).getElementsByTagName("controlURL").item(0).getTextContent());
                serviceType = findNode(doc.getDocumentElement(), "device", "serviceList", "service", "serviceType").getTextContent();

            }else{
                throw new IOException("Required XML structure not found in device description");
            }

        }catch(ParserConfigurationException | SAXException e){
            e.printStackTrace();
            throw new IOException("Failed to parse response.");
        }
    }

    public boolean openPort(int port, Protocol protocol){
        return false;
    }

    public boolean closePort(int port, Protocol protocol){
        return false;
    }

    public boolean isMapped(int port, Protocol protocol){
        return false;
    }

    public InetAddress getExternalIP(){
        return null;
    }

    public Map<String, String> command(String action, Map<String, String> params){
        return null;
    }

    private static Node findNode(Node root, String... path){
        Node current = root;
        for(String tagName : path){
            current = getChildByTag(current, tagName);
            if(current == null){
                return null;
            }
        }
        return current;
    }

    private static Node getChildByTag(Node parent, String tagName){
        NodeList children = parent.getChildNodes();
        for(int i = 0; i < children.getLength(); i++){
            Node child = children.item(i);
            if(child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(tagName)){
                return child;
            }
        }
        return null;
    }
}
