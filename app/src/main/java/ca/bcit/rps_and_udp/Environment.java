package ca.bcit.rps_and_udp;


import android.media.AudioFormat;

public class Environment {
    private static final String HOBOTECH = "192.168.1.71";
    private static final String LINUX = "192.168.1.76";

    public static final String HOST = HOBOTECH;

    public static final String  SERVER          = HOST;
    public static final int     PORT            = 2034;
    public static final int     SAMPLE_RATE     = 10000;                         //samples/second
    public static final int     CHANNELS        = 1;                             //mono
    public static final int     BIT_DEPTH       = AudioFormat.ENCODING_PCM_16BIT;//bits, I think
    public static final double  TIME_FRAME      = 0.5;                           //seconds
    public static final int     BUFFER_FACTOR   = 10;
    public static final int     SAMPLE_SIZE     = 2;
    public static final int     SAMPLE_INTERVAL = 20;                            //units??
    public static final int     BUF_SIZE        = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2;


}
