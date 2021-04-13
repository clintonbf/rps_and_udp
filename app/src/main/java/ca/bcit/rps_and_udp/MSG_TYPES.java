package ca.bcit.rps_and_udp;

public enum MSG_TYPES {
    CONFIRMATION(1),
    INFORMATION(2),
    META_ACTION(3),
    GAME_ACTION(4);

    private byte msg_type;

    MSG_TYPES(byte msg_type) {
    }

    MSG_TYPES(int i) {
    }

    public byte getMsg_type() {
        return msg_type;
    }
}
