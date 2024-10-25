package org.octorrent.jlibupnp;

import java.io.IOException;
import java.net.InetAddress;

import static org.octorrent.jlibupnp.Protocol.TCP;

public class Main {

    public static void main(String[] args)throws IOException {
        UPnP uPnP = new UPnP(InetAddress.getByAddress(new byte[]{ (byte) 192, (byte) 168, 8, 14 }));
        System.out.println(uPnP.getExternalIP().toString());
        //System.out.println("OPEN: "+uPnP.openPort(4040, TCP));
        System.out.println("MAPPED: "+uPnP.isMapped(4040, TCP));
        //System.out.println("CLOSE: "+uPnP.closePort(4040, TCP));
    }
}
