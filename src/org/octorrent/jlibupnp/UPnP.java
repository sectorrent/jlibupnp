package org.octorrent.jlibupnp;

import java.io.IOException;
import java.net.*;

public class UPnP {

    private Gateway gateway;

    public static final String[] REQUESTS = {
            "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\n\r\n",
            "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: urn:schemas-upnp-org:service:WANIPConnection:1\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\n\r\n",
            "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: urn:schemas-upnp-org:service:WANPPPConnection:1\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\n\r\n"
    };

    public UPnP(InetAddress localAddress)throws SocketException {
        DatagramSocket socket = new DatagramSocket(new InetSocketAddress(localAddress, 0));
        socket.setSoTimeout(2000);

        for(String req : REQUESTS){
            try{
                InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(new byte[]{ (byte) 239, (byte) 255, (byte) 255, (byte) 250 }), 1900);
                socket.send(new DatagramPacket(req.getBytes(), 0, req.getBytes().length, address));

                DatagramPacket packet = new DatagramPacket(new byte[65535], 65535);
                socket.receive(packet);

                if(packet != null){
                    gateway = new Gateway(packet.getData(), packet.getLength(), packet.getAddress());
                    return;
                }

            }catch(IOException e){
                e.printStackTrace();
                break;
            }
        }
    }

    public boolean openPort(int port, Protocol protocol)throws IOException {
        return gateway.openPort(port, protocol);
    }

    public boolean closePort(int port, Protocol protocol)throws IOException {
        return gateway.closePort(port, protocol);
    }

    public boolean isMapped(int port, Protocol protocol)throws IOException {
        return gateway.isMapped(port, protocol);
    }

    public InetAddress getExternalIP()throws IOException {
        return gateway.getExternalIP();
    }
}
