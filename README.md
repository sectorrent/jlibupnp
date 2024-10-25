jlibupnp
=====

This is a Java implementation of UPnP as a library. You can easily add this to your project.

If you want to use the [Rust](https://github.com/octorrent/rlibupnp) version.

Implementation
-----
Below are some of the commands you can use:

```java
UPnP uPnP = new UPnP(InetAddress.getByAddress(new byte[]{ 4, 4, 4, 4 }));
System.out.println(uPnP.getExternalIP().toString());
System.out.println("OPEN: "+uPnP.openPort(4040, TCP));
System.out.println("MAPPED: "+uPnP.isMapped(4040, TCP));
System.out.println("CLOSE: "+uPnP.closePort(4040, TCP));

//FOR UDP
System.out.println("MAPPED: "+uPnP.isMapped(4040, UDP));
```
