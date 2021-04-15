package ca.bcit.rps_and_udp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SendAVoiceMemo extends AppCompatActivity {
    private boolean mic = false;
    private InetAddress address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_a_voice_memo);
    }

    public void sendAMemo() {
        this.checkRecordPermission();

        mic = true;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                AudioRecord audioRecorder = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        Environment.SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        Environment.BIT_DEPTH,
                        AudioRecord.getMinBufferSize(
                                Environment.SAMPLE_RATE,
                                AudioFormat.CHANNEL_IN_MONO,
                                Environment.BIT_DEPTH) * Environment.BUFFER_FACTOR);


                int bytesRead = 0;
                byte[] buffer= new byte[Environment.BUF_SIZE];
                try {
                    address = InetAddress.getByName(Environment.SERVER);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

                //First establish connection
                try {
                    DatagramSocket socket = new DatagramSocket();
                    byte[] init = new byte[0];
                    final String initMsg = "Hello";
                    init = initMsg.getBytes();
                    DatagramPacket packet = new DatagramPacket(init, init.length, address, Environment.PORT);
                    socket.send(packet);
                    socket.disconnect();
                    socket.close();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    DatagramSocket socket = new DatagramSocket();

                    audioRecorder.startRecording();

                    while (mic) {
                        bytesRead = audioRecorder.read(buffer, 0, Environment.BUF_SIZE);

                        DatagramPacket packet = new DatagramPacket(buffer, bytesRead, address, Environment.PORT);
                        socket.send(packet);
                        Thread.sleep(Environment.SAMPLE_INTERVAL, 0);
                    }

                    audioRecorder.stop();

//                    Toast.makeText(SendAVoiceMemo.this, "Recording stopped", Toast.LENGTH_LONG).show();

                    audioRecorder.release();
                    socket.disconnect();
                    socket.close();
                    mic = false;
                    return;
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    private void checkRecordPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    123);
        }
    }

    public void toggleMic(View v) {
        this.mic = ! this.mic;
    }

    public void startRecording(View v) {
        this.mic = true;
        sendAMemo();
    }

    public void turnMicOff(View v) {
        this.mic = false;

        Log.i("SEND MEMO", "received signal to mute mic");
    }
}