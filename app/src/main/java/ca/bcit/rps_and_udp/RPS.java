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
    private static final String STREAM  = "STREAM";
    private static final String CLICK   = "CLICK";
    private static final String INVITE  = "INVITE";
    private static final String PLAY    = "PLAY";
    private static final String OUTCOME = "OUTCOME";

    private Button          connect;
    private BufferedReader  fromServer;
    private PrintStream     toServer;
    private InputStream     stream;
    private Socket          socket;
    private GameData        player;
    private Button[]        buttons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_r_p_s);

        Button rock     = (Button) findViewById(R.id.choose_rock);
        Button paper    = (Button) findViewById(R.id.choose_paper);
        Button scissors = (Button) findViewById(R.id.choose_scissors);
        connect         = (Button) findViewById(R.id.connect_rps);

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
                         try {
                            // connectToServer()
                            socket = new Socket(Environment.HOST, Environment.PORT);
                            toServer = new PrintStream(socket.getOutputStream());
                            fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream())); // this could be simplified to just an InputStream (Readers are for char data, and Buffered... we're not dealing with enough data
                            stream = socket.getInputStream();

                            final int uid              = 0;
                            final int CONFIRMATION     = 1;
                            final int CONFIRM_RULESET  = 1;
                            final int payload_length   = 2;
                            final int PROTOCOL_VERSION = 1;
                            final int GAME_ID          = 2;

                            final int[] handshakePayload = { PROTOCOL_VERSION, GAME_ID };

                            RequestPacket req = new RequestPacket(uid, CONFIRMATION, CONFIRM_RULESET, payload_length, handshakePayload);
                            final byte[] handshake = req.toBytes();

                            toServer.write(handshake);

                            while (true) {
                                try {
                                    if (stream.available() > 0) {
                                        byte[] packet = new byte[4];  // TODO: recombine 4 * 1 byte into a 32-bit (watch out for endianness, look at how you send the thing!

                                        int bytesRead = stream.read(packet);

                                        if (bytesRead < 0) {
                                            Log.e(STREAM, "No data received in handshake");
                                            socket.close();
                                            System.exit(1);
                                        }
                                        int[] welcomePayload = new int[packet[2]];
                                        int index = 0;

                                        for (int i = 3; i < 3 + packet[2]; i++) {
                                            welcomePayload[index] = packet[i];
                                            index++;
                                        }

                                        Packet welcomePacket = new Packet(packet[0], packet[1], packet[2], welcomePayload);

                                        player = new GameData(welcomePacket.getPayload()[0]);
                                        Log.i(STREAM, welcomePacket.toString());

                                        //Now an UPDATE message should arrive, inviting play.

                                        bytesRead = stream.read(packet);
                                        if (bytesRead < 0) {
                                            Log.e(STREAM, "No data received in handshake");
                                            socket.close();
                                            System.exit(1);
                                        }

                                        int[] updatePayload = new int[packet[2]];
                                        index = 0;

                                        for (int i = 3; i < 3 + packet[2]; i++) {
                                            updatePayload[index] = packet[i];
                                            index++;
                                        }

                                        Packet invitePacket = new Packet(packet[0], packet[1], packet[2], updatePayload);
                                        Log.i(INVITE, invitePacket.toString());

                                        //Send your play

                                        //This is janky, hard-coded nonsense
                                        final int GAME_ACTION      = 4;
                                        final int MOVE_MADE        = 2;
                                        final int play_payload_length   = 1;

                                        final int[] payload = { 1 };

                                        RequestPacket playReq = new RequestPacket(player.getUid(), GAME_ACTION, MOVE_MADE, play_payload_length, payload);
                                        Log.i(PLAY, playReq.toString());
                                        final byte[] playPacket = req.toBytes();
                                        toServer.write(playPacket);

                                        byte[] outcomePacketFromServer = new byte[5];
                                        bytesRead = stream.read(outcomePacketFromServer);
                                        if (bytesRead < 0) {
                                            Log.e(STREAM, "No data received in game outcome");
                                            socket.close();
                                            System.exit(1);
                                        }

                                        int[] outcomePayload = new int[outcomePacketFromServer[2]];
                                        index = 0;
                                        for (int i = 3; i < 3 + outcomePacketFromServer[2]; i++) {
                                            outcomePayload[index] = outcomePacketFromServer[i];
                                            index++;
                                        }

                                        Log.i(STREAM, "Received game outcome" + outcomePayload[0]);

                                        Packet outcomePacket = new Packet(packet[0], packet[1], packet[2], outcomePayload);

                                        switch (outcomePacket.getMessageContext()) {
                                            case 1:
                                                Log.i(OUTCOME, "Win. Opponent played" + outcomePayload[1]);
                                                break;
                                            case 2:
                                                Log.i(OUTCOME, "Loss. Opponent played" + outcomePayload[1]);
                                                break;
                                            case 3:
                                                Log.i(OUTCOME, "Tie. Opponent played" + outcomePayload[1]);
                                                break;
                                            default:
                                                Log.i(OUTCOME, "ERROR");
                                                break;
                                        }

                                        Log.i(OUTCOME, "Bye");
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
        final int uid              = 0;
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