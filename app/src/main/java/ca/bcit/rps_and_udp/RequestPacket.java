package ca.bcit.rps_and_udp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class RequestPacket extends Packet {
    private int uid;

    public RequestPacket() {}

    public RequestPacket(final int uid, final int messageType, final int messageContext,
                         final int payloadLength, final int[] payload) {
        super(messageType, messageContext, payloadLength, payload);
        this.uid = uid;
    }

    @Override
    public byte[] toBytes() {
        ByteBuffer bb = ByteBuffer.allocate(9); //TODO: Sometimes 9
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(uid);
        bb.put((byte) messageType);
        bb.put((byte) messageContext);
        bb.put((byte) payloadLength);

        for (int payloadItem : payload) {
            bb.put((byte) payloadItem);
        }

        return bb.array();
    }

    @Override
    public String toString() {
        return "RequestPacket{" +
                "uid=" + uid +
                ", messageType=" + messageType +
                ", messageContext=" + messageContext +
                ", payloadLength=" + payloadLength +
                ", payload=" + Arrays.toString(payload) +
                '}';
    }

}
