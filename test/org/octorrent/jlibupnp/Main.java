package org.octorrent.jlibupnp;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args)throws UnknownHostException, SocketException {
        UPnP uPnP = new UPnP(InetAddress.getByAddress(new byte[]{ (byte) 192, (byte) 168, 8, 14 }));

        //InetAddress.getByAddress(new byte[]{ 4, 4, 4, 4 });
    }
}
