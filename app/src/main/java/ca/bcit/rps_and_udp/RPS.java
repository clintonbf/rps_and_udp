package ca.bcit.rps_and_udp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
    private static final String STREAM = "STREAM";
    private static final String CLICK = "CLICK";
    private static final String INVITE = "INVITE";
    private static final String PLAY = "PLAY";

    private Button rock;
    private Button paper;
    private Button scissors;
    private Button connect;
    private BufferedReader fromServer;
    private PrintStream toServer;
    private Socket socket;
    private GameData player;

    private Button[] buttons;

    public boolean running = false;
    public BufferedReader in;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_r_p_s);

        rock        = (Button) findViewById(R.id.choose_rock);
        paper       = (Button) findViewById(R.id.choose_paper);
        scissors    = (Button) findViewById(R.id.choose_scissors);
        connect     = (Button) findViewById(R.id.connect_rps);

        buttons = new Button[3];
        buttons[Choices.ROCK.ordinal()]     = rock;
        buttons[Choices.PAPER.ordinal()]    = paper;
        buttons[Choices.SCISSORS.ordinal()] = scissors;

        player = new GameData();

        disableButtons();

        buttons[Choices.ROCK.ordinal()].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(CLICK, "Clicked " + Choices.ROCK);
                final int choice = 1;

                sendPlay(choice);
            }
        });

        buttons[Choices.PAPER.ordinal()].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(CLICK, "Clicked " + Choices.PAPER);
                final int choice = 2;

                sendPlay(choice);
            }
        });

        buttons[Choices.SCISSORS.ordinal()].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(CLICK, "Clicked " + Choices.SCISSORS);
                final int choice = 3;
                sendPlay(choice);
            }
        });

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableButtons();
                connect.setEnabled(false);

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        running = true;

                        try {
                            socket = new Socket(Environment.HOST, Environment.PORT);
                            toServer = new PrintStream(socket.getOutputStream());
                            fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream())); // this could be simplified to just an InputStream (Readers are for char data, and Buffered... we're not dealing with enough data

                            final byte uid1             = 0;
                            final byte uid2             = 0;
                            final byte uid3             = 0;
                            byte uid4                   = 0;
                            final byte CONFIRMATION     = 1;
                            final byte CONFIRM_RULESET  = 1;
                            final byte payload_length   = 2;
                            final byte PROTOCOL_VERSION = 1;
                            final byte GAME_ID          = 2;

                            byte[] handshake = {uid1, uid2, uid3, uid4, CONFIRMATION, CONFIRM_RULESET, payload_length, PROTOCOL_VERSION, GAME_ID};

                            toServer.write(handshake);

                            while (true) {
                                try {
                                    InputStream stream = socket.getInputStream();

                                    if (stream.available() > 0) {
                                        byte[] packet = new byte[4];

                                        int bytesRead = stream.read(packet);

                                        if (bytesRead < 0) {
                                            Log.e(STREAM, "No data received in handshake");
                                            socket.close();
                                            System.exit(1);
                                        }
                                        int[] payload = new int[packet[2]];
                                        int index = 0;

                                        for (int i = 3; i < 3 + packet[2]; i++) {
                                            payload[index] = packet[i];
                                            index++;
                                        }

                                        Packet welcomePacket = new Packet(packet[0], packet[1], packet[2], payload);

                                        player = new GameData(welcomePacket.getPayload()[0]);
                                        Log.i(STREAM, welcomePacket.toString());

                                        //Now and UPDATE message should arrive, inviting play.

                                        bytesRead = stream.read(packet);
                                        if (bytesRead < 0) {
                                            Log.e(STREAM, "No data received in handshake");
                                            socket.close();
                                            System.exit(1);
                                        }

                                        payload = new int[packet[2]];
                                        index = 0;

                                        for (int i = 3; i < 3 + packet[2]; i++) {
                                            payload[index] = packet[i];
                                            index++;
                                        }

                                        Packet invitePacket = new Packet(packet[0], packet[1], packet[2], payload);
                                        Log.i(INVITE, invitePacket.toString());

                                        //Send your play
//                                        enableButtons();

                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (SocketException se) {
                            Log.e("CONNECTION:", "Unable to connect");
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
        final int CONFIRMATION     = 1;
        final int CONFIRM_RULESET  = 1;
        final int payload_length   = 2;
        final int PROTOCOL_VERSION = 1;
        final int GAME_ID          = 2;

        final int[] payload = { PROTOCOL_VERSION, GAME_ID };

        RequestPacket req = new RequestPacket(uid, CONFIRMATION, CONFIRM_RULESET, payload_length, payload);
        final byte[] packet = req.toBytes();

        Thread thread = null;
        try {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        toServer.write(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        thread.start();
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

    private void sendPlay(final int choice) {
        final int GAME_ACTION      = 4;
        final int MOVE_MADE        = 2;
        final int payload_length   = 1;

        final int[] payload = { choice };

        disableButtons();

        RequestPacket req = new RequestPacket(player.getUid(), GAME_ACTION, MOVE_MADE, payload_length, payload);
        Log.i(PLAY, req.toString());
        final byte[] packet = req.toBytes();

        Thread thread = null;
        try {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        toServer.write(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        thread.start();
    }

    private void sendPacket(final byte[] packet) {
        Thread thread = null;
        try {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        toServer.write(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        thread.start();
    }

    private void disableButtons() {
        for (Button button : this.buttons) {
            button.setEnabled(false);
        }
    }

    private void enableButtons() {
        for (Button button : this.buttons) {
            button.setEnabled(true);
        }
    }

}