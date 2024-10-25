package org.octorrent.jlibupnp;

import java.io.IOException;
import java.net.InetAddress;

public class Main {

    public static void main(String[] args)throws IOException {
        UPnP uPnP = new UPnP(InetAddress.getByAddress(new byte[]{ (byte) 192, (byte) 168, 8, 14 }));
        uPnP.getExternalIP();

        //InetAddress.getByAddress(new byte[]{ 4, 4, 4, 4 });
    }
}
