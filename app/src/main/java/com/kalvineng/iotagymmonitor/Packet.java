package com.kalvineng.iotagymmonitor;

public class Packet {
    String hr;
    String ba;
    String bb;
    String bc;

    public Packet(String hr, String ba, String bb, String bc) {
        this.hr = hr;
        this.ba = ba;
        this.bb = bb;
        this.bc = bc;
    }

    public String getHr() {
        return hr;
    }

    public String getBb() {
        return bb;
    }

    public String getBa() {
        return ba;
    }

    public String getBc() {
        return bc;
    }
}
