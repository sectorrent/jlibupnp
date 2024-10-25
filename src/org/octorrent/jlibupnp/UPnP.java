package org.octorrent.jlibupnp;

import java.net.InetAddress;

public class UPnP {

    private Gateway gateway;

    public static final String[] REQUESTS = {
            "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\n\r\n",
            "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: urn:schemas-upnp-org:service:WANIPConnection:1\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\n\r\n",
            "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: urn:schemas-upnp-org:service:WANPPPConnection:1\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\n\r\n"
    };

    public UPnP(){

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
}
