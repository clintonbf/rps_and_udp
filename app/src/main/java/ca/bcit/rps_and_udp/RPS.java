package ca.bcit.rps_and_udp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

enum Choices {
    ROCK,
    PAPER,
    SCISSORS
};

enum Outcomes {
    WIN,
    LOSS,
    TIE
};

public class RPS extends AppCompatActivity {
    private static final String STREAM  = "STREAM";
    private static final String CLICK   = "CLICK";
    private static final String INVITE  = "INVITE";
    private static final String PLAY    = "PLAY";
    private static final String OUTCOME = "OUTCOME";

    private Button          connectButton;
    private PrintStream     toServer;
    private InputStream     fromServer;
    private TextView        outcome;
    private Socket          socket;
    private GameData        player;
    private Button[]        buttons;
    private int             playChoice = 10000;
    private int             gameOutcome;
    private int             opponentsMove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_r_p_s);

        Button rock     = (Button) findViewById(R.id.choose_rock);
        Button paper    = (Button) findViewById(R.id.choose_paper);
        Button scissors = (Button) findViewById(R.id.choose_scissors);
        connectButton   = (Button) findViewById(R.id.connect_rps);
        outcome         = (TextView) findViewById(R.id.outcome_label);

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

//                sendPlay(choice);
                playChoice = choice;
                notifyUserOfOutcome();
                initButtons();
            }
        });

        buttons[Choices.PAPER.ordinal()].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(CLICK, "Clicked " + Choices.PAPER);
                final int choice = 2;

//                sendPlay(choice);
                playChoice = choice;
                notifyUserOfOutcome();
                initButtons();
            }
        });


        buttons[Choices.SCISSORS.ordinal()].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(CLICK, "Clicked " + Choices.SCISSORS);
                final int choice = 3;

//                sendPlay(choice);
                playChoice = choice;
                notifyUserOfOutcome();
                initButtons();
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableButtons();
                connectButton.setEnabled(false);

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                         try {
                            /*
                             connectToServer()
                             */
                            socket = new Socket(Environment.HOST, Environment.PORT);
                            toServer = new PrintStream(socket.getOutputStream());
                            fromServer = socket.getInputStream();
                            /*
                             /connectToServer()
                             */
//                             new connectToServerClass().execute();

                             /*
                             sendHandshake()
                              */
                            final int uid              = 0;
                            final int CONFIRMATION     = 1;
                            final int CONFIRM_RULESET  = 1;
                            final int payload_length   = 2;
                            final int PROTOCOL_VERSION = 1;
                            final int GAME_ID          = 2;

                            final int[] handshakePayload = { PROTOCOL_VERSION, GAME_ID };

                            RequestPacket req = new RequestPacket(uid, CONFIRMATION,
                                    CONFIRM_RULESET, payload_length, handshakePayload);
                            final byte[] handshake = req.toBytes();

                            toServer.write(handshake);
                            /*
                            / sendHandshake()
                             */

                            while (true) {
                                try {
                                    if (fromServer.available() > 0) {
                                        /*
                                        setUid()
                                         */
                                        byte[] packet = new byte[7];

                                        int bytesRead = fromServer.read(packet);

                                        ByteBuffer bb = ByteBuffer.wrap(packet);

                                        if (bytesRead < 0) {
                                            Log.e(STREAM, "No data received in handshake");
                                            socket.close();
                                            System.exit(1);
                                        }

                                        int[] welcomePayload = new int[bb.get(2)];

                                        welcomePayload[0] = bb.getInt(3);

                                        Packet welcomePacket = new Packet((int)bb.get(0), (int)bb.get(1), (int)bb.get(2), welcomePayload);

                                        player = new GameData(welcomePacket.getPayload()[0]);
                                        Log.i(STREAM, welcomePacket.toString());
                                        Log.i(STREAM, player.toString());
                                        /*
                                        / setUid()
                                         */

                                        /*
                                        getPlayInvitation()
                                         */

                                        bytesRead = fromServer.read(packet);
                                        if (bytesRead < 0) {
                                            Log.e(STREAM, "No data received in handshake");
                                            socket.close();
                                            System.exit(1);
                                        }

                                        int[] updatePayload = new int[packet[2]];
                                        int index = 0;

                                        for (int i = 3; i < 3 + packet[2]; i++) {
                                            updatePayload[index] = packet[i];
                                            index++;
                                        }

                                        Packet invitePacket = new Packet(packet[0], packet[1], packet[2], updatePayload);
                                        Log.i(INVITE, invitePacket.toString());
                                        /*
                                        / getPlayInvitation()
                                         */

                                        //Send your play

                                        /*
                                        janky-ass nonsense
                                         */

                                        while (playChoice == 10000);

                                        final int GAME_ACTION      = 4;
                                        final int MOVE_MADE        = 2;
                                        final int play_payload_length   = 1;

                                        final int[] payload = { playChoice };

                                        RequestPacket playReq = new RequestPacket(player.getUid(), GAME_ACTION, MOVE_MADE, play_payload_length, payload);
                                        Log.i(PLAY, playReq.toString());
                                        final byte[] playPacket = req.toBytes();
                                        toServer.write(playPacket);
                                        /*
                                        / janky-ass nonsense
                                         */

                                        byte[] outcomePacketFromServer = new byte[5];
                                        bytesRead = fromServer.read(outcomePacketFromServer);
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

                                        Log.i(STREAM, "Received game outcome " + outcomePayload[0]);

                                        Packet outcomePacket = new Packet(packet[0], packet[1], packet[2], outcomePayload);
                                        opponentsMove = outcomePayload[1];

                                        switch (outcomePacket.payload[0]) {
                                            case 1:
                                                Log.i(OUTCOME, "Win. Opponent played " + outcomePayload[1]);
                                                gameOutcome = Outcomes.WIN.ordinal();
                                                break;
                                            case 2:
                                                Log.i(OUTCOME, "Loss. Opponent played " + outcomePayload[1]);
                                                gameOutcome = Outcomes.LOSS.ordinal();

                                                break;
                                            case 3:
                                                Log.i(OUTCOME, "Tie. Opponent played " + outcomePayload[1]);
                                                gameOutcome = Outcomes.TIE.ordinal();

                                                break;
                                            default:
                                                Log.i(OUTCOME, "ERROR");
                                                break;
                                        }

                                        Log.i(OUTCOME, "Bye");
                                        socket.close();
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


    private class connectToServerClass extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            connectToServer();

            return null;
        }
    }

    private void connectToServer() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(Environment.HOST, Environment.PORT);
                    toServer = new PrintStream(socket.getOutputStream());
//                    fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

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
//        try {
//            String message = stream.read();
//            Log.i("UID", "Server sent uid " + message);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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

    private void initButtons() {
        connectButton.setEnabled(true);
        disableButtons();
    }

    private void notifyUserOfOutcome() {
        switch (gameOutcome){
            case 0:
                outcome.setText("WIN! Opponent played " + getTextOfPlay(opponentsMove));
                break;
            case 1:
                outcome.setText("WIN! Opponent played " +  getTextOfPlay(opponentsMove));
                break;
            case 2:
                outcome.setText("WIN! Opponent played " +  getTextOfPlay(opponentsMove));
                break;
             default:
                 break;
        }
    }

    private String getTextOfPlay(final int play) {
        if (play == 1) {
            return "Rock";
        }

        if (play == 2) {
            return "Paper";
        }

        return "Scissors";
    }
}