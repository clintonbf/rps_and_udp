package ca.bcit.rps_and_udp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

enum Choices {
    ROCK,
    PAPER,
    SCISSORS
}

public class RPS extends AppCompatActivity {
    private Button paper;
    private Button scissors;
    private Button connect;
    private BufferedReader fromServer;
    private PrintStream toServer;
    private Socket socket;
    private GameData player;

    private Button[] buttons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_r_p_s);

        Button rock = (Button) findViewById(R.id.choose_rock);
        paper = (Button) findViewById(R.id.choose_paper);
        scissors = (Button) findViewById(R.id.choose_scissors);
        connect = (Button) findViewById(R.id.connect_rps);

        buttons = new Button[3];
        buttons[0] = rock;
        buttons[1] = paper;
        buttons[2] = scissors;

        disableButtons();

        player = new GameData();

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                connectToServer();
//                sendHandshake();
//                setUid();
//                enableButtons();

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            socket = new Socket(Environment.HOST, Environment.PORT);
                            toServer = new PrintStream(socket.getOutputStream());
                            fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                            final int uid               = 0;
                            final byte CONFIRMATION     = 1;
                            final byte CONFIRM_RULESET  = 1;
                            final byte payload_length   = 2;
                            final byte PROTOCOL_VERSION = 1;
                            final byte GAME_ID          = 2;

                            byte[] message = { uid, CONFIRMATION, CONFIRM_RULESET, payload_length, PROTOCOL_VERSION, GAME_ID};

                            toServer.println(Arrays.toString(message));

                            try {
                                System.out.println("HI!!!!!");
                                String message2 = fromServer.readLine();
                                Log.i("UID", "Server sent uid " + message2);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        } catch (SocketException se) {
                            Log.e("CONNECTION:", "Unable to connect");
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                thread.start();
            }
        });
    }

    /**
     * Opening packet
     * [ 4 bytes ] | [1 byte ] | [1 byte ] | [ 1 byte ] | [ 2 bytes ]
     *
     * Representing
     * [ uid  ] | [ Confirmation ] | [ Ruleset ] | [ Payload length ] | [ protocol version game id ]
     *
     *
     * [ 0 ] | [ 1 ] | [ 1 ] | [ 1 ] | [ 1 2 ]
     */

    private void connectToServer() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(Environment.HOST, Environment.PORT);
                    toServer = new PrintStream(socket.getOutputStream());
                    fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                } catch (SocketException se) {
                    Log.e("CONNECTION:", "Unable to connect");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    private void sendHandshake() {
        final int uid               = 0;
        final byte CONFIRMATION     = 1;
        final byte CONFIRM_RULESET  = 1;
        final byte payload_length   = 2;
        final byte PROTOCOL_VERSION = 1;
        final byte GAME_ID          = 2;

        byte[] message = { uid, CONFIRMATION, CONFIRM_RULESET, payload_length, PROTOCOL_VERSION, GAME_ID};
        if (toServer == null) {
            try {
                toServer = new PrintStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        toServer.println(Arrays.toString(message));
    }

    private void setUid() {
        //Read a message
        try {
            String message = fromServer.readLine();
            Log.i("UID", "Server sent uid " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disableButtons() {
        for (Button button : this.buttons) {
            button.setEnabled(false);
        }

        connect.setEnabled(true);
    }

    private void enableButtons() {
        for (Button button : this.buttons) {
            button.setEnabled(true);
        }

        connect.setEnabled(false);
    }
}