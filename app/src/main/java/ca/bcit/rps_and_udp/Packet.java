package ca.bcit.rps_and_udp;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class Packet {
    protected int messageType;
    protected int messageContext;
    protected int payloadLength;
    protected int[] payload;

    public Packet() {}

    public Packet(final int messageType, final int messageContext, final int payloadLength,
                  final int[] payload) {
        this.messageType = messageType;
        this.messageContext = messageContext;
        this.payloadLength = payloadLength;
        this.payload = payload;
    }

    public int getMessageType() {
        return messageType;
    }

    public int getMessageContext() {
        return messageContext;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public int[] getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "messageType=" + messageType +
                ", messageContext=" + messageContext +
                ", payloadLength=" + payloadLength +
                ", payload=" + Arrays.toString(payload) +
                '}';
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeInt(this.messageType);
        dos.writeInt(this.messageContext);
        dos.writeInt(this.payloadLength);

        for (int payloadItem : this.payload) {
            dos.writeInt(payloadItem);
        }

        dos.flush(); // Where specifically does this need to go??
        return bos.toByteArray();
    }
}
