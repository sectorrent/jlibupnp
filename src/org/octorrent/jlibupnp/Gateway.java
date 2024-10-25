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
                serviceType = findNode(doc.getDocumentElement(), "device", "serviceList", "service", "serviceType").getTextContent();

            }else{
                throw new IOException("Required XML structure not found in device description");
            }

        }catch(ParserConfigurationException | SAXException e){
            e.printStackTrace();
            throw new IOException("Failed to parse response.");
        }
    }

    public boolean openPort(int port, Protocol protocol)throws IOException {
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

        command("AddPortMapping", params);
        return true;
    }

    public boolean closePort(int port, Protocol protocol)throws IOException {
        if(port < 1 || port > 65535){
            throw new IllegalArgumentException("Port is out of range.");
        }

        Map<String, String> params = new HashMap<>();
        params.put("NewRemoteHost", "");
        params.put("NewProtocol", protocol.value());
        params.put("NewExternalPort", port+"");

        command("DeletePortMapping", params);
        return true;
    }

    public boolean isMapped(int port, Protocol protocol)throws IOException {
        if(port < 1 || port > 65535){
            throw new IllegalArgumentException("Port is out of range.");
        }

        Map<String, String> params = new HashMap<>();
        params.put("NewRemoteHost", "");
        params.put("NewProtocol", protocol.value());
        params.put("NewExternalPort", port+"");

        Map<String, String> response = command("GetSpecificPortMappingEntry", params);
        return response.get("NewEnabled").equals("1");
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

        String request = "GET "+controlUrl.getPath()+" HTTP/1.1\r\n" +
                "Host: "+controlUrl.getHost()+"\r\n" +
                "Content-Type: text/xml\r\n" +
                "SOAPAction: \""+serviceType+"#"+action+"\"\r\n" +
                "Content-Length: "+soap.toString().getBytes().length+"\r\n\r\n";
        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.write(soap.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();

        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        boolean xmlStart = false;
        while((line = reader.readLine()) != null){
            if(line.isEmpty()){
                xmlStart = true;
                continue;
            }

            if(xmlStart){
                response.append(line).append("\n");
            }
        }
        socket.close();

        System.out.println(response);

        /*
        let doc = roxmltree::Document::parse(response_str.split("\r\n\r\n").last().unwrap())
                .map_err(|e| io::Error::new(io::ErrorKind::Other, e.to_string()))?;

        if let Some(root) = doc.root_element().descendants().find(|node| node.tag_name().name() == "Body").unwrap()
                .descendants().find(|node| node.tag_name().name() == format!("{}Response", action)) {
            let mut response = HashMap::new();

            let mut iter = root.descendants();
            iter.next();
            while let Some(node) = &iter.next() {
                if node.is_element() {
                    let key = node.tag_name().name().to_string();
                    if let Some(node) = iter.next() {
                        if node.is_text() {
                            response.insert(key, node.text().unwrap().to_string());
                        }
                    }
                }
            }

            return Ok(response);
        }

        let error_code = doc.descendants().find(|node| node.tag_name().name() == "errorCode").unwrap().text().unwrap();
        let error_description = doc.descendants().find(|node| node.tag_name().name() == "errorDescription").unwrap().text().unwrap();

        Err(io::Error::new(io::ErrorKind::Other, format!("{}: {}", error_code, error_description)))
        */
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
