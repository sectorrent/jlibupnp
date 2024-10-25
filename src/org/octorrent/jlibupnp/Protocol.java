package org.octorrent.jlibupnp;

public enum Protocol {

    UDP {
        @Override
        public String value(){
            return "UDP";
        }
    },
    TCP {
        @Override
        public String value(){
            return "TCP";
        }
    };

    public String value(){
        return null;
    }
}
