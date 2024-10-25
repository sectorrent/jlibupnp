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
        OutputStream out = socket.getOutputStream();

        String request = "GET "+url.getPath()+" HTTP/1.1\r\n" +
                "Host: "+url.getHost()+"\r\n" +
                "Content-Type: text/xml\r\n\r\n";
        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.flush();

        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder xmlResponse = new StringBuilder();
        String line;
        boolean xmlStart = false;
        while((line = reader.readLine()) != null){
            if(line.isEmpty()){
                xmlStart = true;
                continue;
            }

            if(xmlStart){
                xmlResponse.append(line).append("\n");
            }
        }
        socket.close();

        try{
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.toString().getBytes(StandardCharsets.UTF_8)));

            Node deviceNode = findNode(doc.getDocumentElement(), "device", "deviceList", "device", "deviceList", "device", "serviceList", "service");

            if(deviceNode != null && deviceNode instanceof Element){
                controlUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), ((Element) deviceNode).getElementsByTagName("controlURL").item(0).getTextContent());
                serviceType = ((Element) deviceNode).getElementsByTagName("serviceType").item(0).getTextContent();

            }else{
                throw new IOException("Required XML structure not found in device description");
            }

        }catch(ParserConfigurationException | SAXException e){
            e.printStackTrace();
            throw new IOException("Failed to parse response.");
        }
    }

    public boolean openPort(int port, Protocol protocol){
        if(port < 1 || port > 65535){
            throw new IllegalArgumentException("Port is out of range.");
        }

        Map<String, String> params = new HashMap<>();
        params.put("NewRemoteHost", "");
        params.put("NewProtocol", protocol.value());
        params.put("NewInternalClient", address.getHostAddress());
        params.put("NewExternalPort", port+"");
        params.put("NewInternalPort", port+"");
        params.put("NewEnabled", "1");
        params.put("NewPortMappingDescription", "UPnP");
        params.put("NewLeaseDuration", "0");

        try{
            command("AddPortMapping", params);
            return true;
        }catch(IOException e){
            return false;
        }
    }

    public boolean closePort(int port, Protocol protocol){
        if(port < 1 || port > 65535){
            throw new IllegalArgumentException("Port is out of range.");
        }

        Map<String, String> params = new HashMap<>();
        params.put("NewRemoteHost", "");
        params.put("NewProtocol", protocol.value());
        params.put("NewExternalPort", port+"");

        try{
            command("DeletePortMapping", params);
            return true;
        }catch(IOException e){
            return false;
        }
    }

    public boolean isMapped(int port, Protocol protocol){
        if(port < 1 || port > 65535){
            throw new IllegalArgumentException("Port is out of range.");
        }

        Map<String, String> params = new HashMap<>();
        params.put("NewRemoteHost", "");
        params.put("NewProtocol", protocol.value());
        params.put("NewExternalPort", port+"");

        try{
            Map<String, String> response = command("GetSpecificPortMappingEntry", params);
            return response.get("NewEnabled").equals("1");
        }catch(IOException e){
            return false;
        }
    }

    public InetAddress getExternalIP()throws IOException {
        Map<String, String> response = command("GetExternalIPAddress", null);
        return InetAddress.getByName(response.get("NewExternalIPAddress"));
    }

    private Map<String, String> command(String action, Map<String, String> params)throws IOException {
        StringBuilder soap = new StringBuilder("<?xml version=\"1.0\"?>\r\n" +
            "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
            "<SOAP-ENV:Body>" +
            "<m:"+action+" xmlns:m=\""+serviceType+"\">");

        if(params != null && !params.isEmpty()){
            for(String key : params.keySet()){
                soap.append("<"+key+">"+params.get(key)+"</m"+key+">");
            }
        }

        soap.append("</m:"+action+"></SOAP-ENV:Body></SOAP-ENV:Envelope>");

        Socket socket = new Socket(controlUrl.getHost(), controlUrl.getPort());
        OutputStream out = socket.getOutputStream();

        System.out.println(controlUrl.toString());

        String request = "POST "+controlUrl.getPath()+" HTTP/1.1\r\n" +
                "Host: "+controlUrl.getHost()+"\r\n" +
                "Content-Type: text/xml\r\n" +
                "SOAPAction: \""+serviceType+"#"+action+"\"\r\n" +
                "Content-Length: "+soap.toString().getBytes().length+"\r\n\r\n";
        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.write(soap.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();

        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder xmlResponse = new StringBuilder();
        String line;
        boolean xmlStart = false;
        while((line = reader.readLine()) != null){
            if(line.isEmpty()){
                xmlStart = true;
                continue;
            }

            if(xmlStart){
                xmlResponse.append(line).append("\n");
            }
        }
        socket.close();

        try{
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.toString().getBytes(StandardCharsets.UTF_8)));

            NodeList bodyList = doc.getElementsByTagName("s:Body");
            if(bodyList.getLength() > 0){
                Element bodyElement = (Element) bodyList.item(0);
                NodeList responseList = bodyElement.getElementsByTagName("u:"+action+"Response");

                if(responseList.getLength() > 0){
                    Element responseElement = (Element) responseList.item(0);

                    Map<String, String> response = new HashMap<>();

                    NodeList children = responseElement.getChildNodes();
                    for(int i = 0; i < children.getLength(); i++){
                        Node node = children.item(i);

                        if(node.getNodeType() == Node.ELEMENT_NODE){
                            String key = node.getNodeName();
                            String value = node.getTextContent();
                            response.put(key, value);
                        }
                    }
                    return response;
                }
            }

            String errorCode = doc.getElementsByTagName("errorCode").item(0).getTextContent();
            String errorDescription = doc.getElementsByTagName("errorDescription").item(0).getTextContent();

            throw new IOException("Error "+errorCode+": "+errorDescription);

        }catch(ParserConfigurationException | SAXException e){
            throw new IOException("Failed to parse XML", e);
        }
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
