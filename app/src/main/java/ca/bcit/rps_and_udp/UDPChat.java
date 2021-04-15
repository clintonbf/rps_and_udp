package ca.bcit.rps_and_udp;

// Adapted from: https://github.com/DeanThomson/android-udp-audio-chat

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPChat extends AppCompatActivity {

    private static final String LOG_TAG = "AudioCall";
    private Button connect;
    private Button chat;
    private Button hang_up;

    DatagramSocket socket;
    private InetAddress address;

    private static final int SAMPLE_RATE = Environment.SAMPLE_RATE;
    private static final int SAMPLE_INTERVAL = Environment.SAMPLE_INTERVAL;
    private static final int SAMPLE_SIZE = Environment.SAMPLE_SIZE;
    private static final int BUF_SIZE = Environment.BUF_SIZE;

    private boolean mic;
    private boolean speakers;

    private static final int NOT_CONNECTED = 1;
    private static final int IS_CONNECTED = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_u_d_p_chat);

        connect = (Button) findViewById(R.id.connect_btn);
        chat = (Button) findViewById(R.id.chat_btn);
        hang_up = (Button) findViewById(R.id.hangup_btn);
        enableButtonsByFunction(NOT_CONNECTED);

        try {
            address = InetAddress.getByName(Environment.SERVER);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        hang_up.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mic = false;
                speakers = false;

                if (socket != null) {
                    socket.close();
                    socket.disconnect();
                    socket = null;

                    enableButtonsByFunction(NOT_CONNECTED);
                }

                Log.i("UDP Connection", "Disconnected from server");
            }
        });

        hang_up.setEnabled(false);
    }

    public void connectToUDPServer(View v) {
        final String message = "Connect";

        final Data data = new Data(message);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] messageB = data.getMessage().getBytes(); //Add code for a song

                    socket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(messageB, messageB.length, address, Environment.PORT);
                    socket.send(packet);

                    Log.i("UDP handshake", "Connected to UDP server");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();
        enableButtonsByFunction(IS_CONNECTED);
    }

    public void startTalking(View v) {
        startMic();
//       startSpeakers();
    }

    public void startListening(View v) {
        startSpeakers();
    }

    private void enableButtonsByFunction(final int function) {
        switch (function) {
            case NOT_CONNECTED:
                connect.setEnabled(true);
                hang_up.setEnabled(false);
                chat.setEnabled(false);
                break;
            case IS_CONNECTED:
                connect.setEnabled(false);
                hang_up.setEnabled(true);
                chat.setEnabled(true);
                break;
            default:
                break;
        }
    }

    public void startMic() {
        // Creates the thread for capturing and transmitting audio
        mic = true;
        Log.i(LOG_TAG, "Mic is active");
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                // Create an instance of the AudioRecord class
                Log.i(LOG_TAG, "Send thread started. Thread id: " + Thread.currentThread().getId());
                AudioRecord audioRecorder = new AudioRecord (MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)*10);

                int bytes_read = 0;
                int bytes_sent = 0;
                byte[] buf = new byte[BUF_SIZE];

                try {
                    // Create a socket and start recording
                    Log.i(LOG_TAG, "Packet destination: " + address.toString());

                    audioRecorder.startRecording();
                    while(mic) {
                        // Capture audio from the mic and transmit it
                        bytes_read = audioRecorder.read(buf, 0, BUF_SIZE);
                        DatagramPacket packet = new DatagramPacket(buf, bytes_read, address, Environment.PORT);
                        socket.send(packet);
                        bytes_sent += bytes_read;
                        Log.i(LOG_TAG, "Total bytes sent: " + bytes_sent);
                        Thread.sleep(SAMPLE_INTERVAL, 0);
                    }
                    // Stop recording and release resources
                    audioRecorder.stop();
                    audioRecorder.release();
                    socket.disconnect();
                    socket.close();
                    mic = false;
                    return;
                }
                catch(InterruptedException e) {
                    Log.e(LOG_TAG, "InterruptedException: " + e.toString());
                    mic = false;
                }
                catch(SocketException e) {
                    Log.e(LOG_TAG, "SocketException: " + e.toString());
                    mic = false;
                }
                catch(UnknownHostException e) {
                    Log.e(LOG_TAG, "UnknownHostException: " + e.toString());
                    mic = false;
                }
                catch(IOException e) {
                    Log.e(LOG_TAG, "IOException: " + e.toString());
                    mic = false;
                }
            }
        });
        thread.start();
    }

    public void startSpeakers() {
        // Creates the thread for receiving and playing back audio
        if(!speakers) {

            speakers = true;
            Log.i(LOG_TAG, "Speaker is active");

            Thread receiveThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    // Create an instance of AudioTrack, used for playing back audio
                    Log.i(LOG_TAG, "Receive thread started. Thread id: " + Thread.currentThread().getId());
                    AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, BUF_SIZE, AudioTrack.MODE_STREAM);
                    track.play();
                    try {
                        // Define a socket to receive the audio
                        DatagramSocket socket = new DatagramSocket(Environment.PORT);
                        byte[] buf = new byte[BUF_SIZE];
                        while(speakers) {
                            // Play back the audio received from packets
                            DatagramPacket packet = new DatagramPacket(buf, BUF_SIZE);
                            socket.receive(packet);
                            Log.i(LOG_TAG, "Packet received: " + packet.getLength());
                            track.write(packet.getData(), 0, BUF_SIZE);
                        }
                        // Stop playing back and release resources
                        socket.disconnect();
                        socket.close();
                        track.stop();
                        track.flush();
                        track.release();
                        speakers = false;
                        return;
                    }
                    catch(SocketException e) {

                        Log.e(LOG_TAG, "SocketException: " + e.toString());
                        speakers = false;
                    }
                    catch(IOException e) {

                        Log.e(LOG_TAG, "IOException: " + e.toString());
                        speakers = false;
                    }
                }
            });
            receiveThread.start();
        }
    }
}