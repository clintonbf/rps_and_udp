package ca.bcit.rps_and_udp;

public class GameData {
    private int uid;

    public GameData() {
    }

    public GameData(final int id) {
        this.uid = id;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        return "GameData{" +
                "uid=" + uid +
                '}';
    }
}