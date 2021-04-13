package ca.bcit.rps_and_udp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Queue;

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
    private Byte choice = 0;

    public boolean running = false;
    public BufferedReader in;

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


        buttons[0].setClickable(false);

        buttons[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choice = 1;

            }
        });
        buttons[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choice = 2;

            }
        });
        buttons[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choice = 3;

            }
        });


//        disableButtons();

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
                        running = true;

                        try {
                            socket = new Socket(Environment.HOST, Environment.PORT);
                            toServer = new PrintStream(socket.getOutputStream());


                            final byte uid1              = 0;
                            final byte uid2              = 0;
                            final byte uid3             = 0;
                            byte uid4              = 0;
                            final byte CONFIRMATION     = 1;
                            final byte CONFIRM_RULESET  = 1;
                            final byte payload_length   = 2;
                            final byte PROTOCOL_VERSION = 1;
                            final byte GAME_ID          = 2;

                            byte[] message = {uid1, uid2, uid3, uid4, CONFIRMATION, CONFIRM_RULESET, payload_length, PROTOCOL_VERSION, GAME_ID};

                            toServer.write(message);



                            System.out.println("hi");

                            try {
                                while(true){
                                    InputStream stream = socket.getInputStream();
                                    if(stream.available() > 0){
                                        byte[] data = new byte[30];
                                        int count = stream.read(data);
                                        if(data[0] == 10) {
                                            // get uid
                                            uid4 = data[7];
                                           buttons[0].setClickable(true);
                                        } else if (data[0] == 20) {
//                                            enableButtons();
                                            while(choice == 0) {
                                                // wait
                                            }
                                            System.out.println("\n\n*********************************************************************************************HWLLO********************************************************************************************************* \n\n");
                                        }
//                                        break;
                                    };
                                    Thread.sleep(1);
                                }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
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


    public final class Reader implements Runnable {

        final private Socket sock;

        public boolean mRun = false;

        public Reader(final Socket socket) {
            sock = socket;
        }

        @Override
        public void run() {
            mRun = true;
            System.out.println("Socket is : " + sock);
            while (mRun) {
                try {
                    fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
//                    byte[] data = new byte[30];
//                    int count = fromServer.read();


//                    if (count > 1) {
//                        mRun = false;
//                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
//            BufferedReader fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));

        }
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