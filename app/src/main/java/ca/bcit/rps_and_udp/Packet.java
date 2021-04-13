package ca.bcit.rps_and_udp;

import java.util.Arrays;

public class Packet {
    private int messageType;
    private int messageContext;
    private int payloadLength;
    private int[] payload;

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
}
