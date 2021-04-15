package ca.bcit.rps_and_udp;

public class Data {
    private String message;
    private int resid;

    public Data(String m) {
        this.message = m;
    }

    public Data(int r) {
        this.resid = r;
    }

    public String getMessage() {
        return message;
    }

    public int getResid() {
        return resid;
    }
}
