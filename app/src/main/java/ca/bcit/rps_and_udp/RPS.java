package ca.bcit.rps_and_udp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

enum Outcomes {
    WIN,
    LOSS,
    TIE
};

public class RPS extends AppCompatActivity {
    private static final String CONNECT     = "CONNECT";
    private static final String SET_UID = "SET UID";
    private static final String STREAM  = "STREAM";
    private static final String CLICK   = "CLICK";
    private static final String RECEIVE_INVITATION = "RECEIVE_INVITATION";
    private static final String SEND_PLAY = "SEND PLAY";
    private static final String PLAY_ACK    = "PLAY Acknowledged";
    private static final String OUTCOME = "OUTCOME";

    private static final int ROCK       = 1;
    private static final int PAPER      = 2;
    private static final int SCISSORS   = 3;

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

        buttons = new Button[4];
        buttons[ROCK]     = rock;
        buttons[PAPER]    = paper;
        buttons[SCISSORS] = scissors;

        player = new GameData();

        disableButtons();

        buttons[ROCK].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processClick(ROCK, "Rock");
            }
        });

        buttons[PAPER].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processClick(PAPER, "Paper");
            }
        });


        buttons[SCISSORS].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processClick(SCISSORS, "Scissors");
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
                        connectToServer();
                        sendHandshake();
                        setUid();
                        getPlayInvitation();

                        //Buttons are already enabled (or enable them now if you can figure cross thread msging

                        while (playChoice == 10000);
                        Log.d(SEND_PLAY, "Play has been chosen by player " + player.getUid());

                        sendPlay();
                        getPlayAcknowledgement();
                        processOutcome();

                        try {
                            toServer.close();
                            fromServer.close();
                            socket.close();
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
         Log.d(CONNECT, "Attempting connection to "+ Environment.HOST);

         try {
            socket = new Socket(Environment.HOST, Environment.PORT);
            toServer = new PrintStream(socket.getOutputStream());
            fromServer = socket.getInputStream();
            socket.setSoTimeout(10*1000);

            Log.d(CONNECT, "Established connection");

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
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

        try {
            toServer.write(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUid() {
        byte[] packet = new byte[7];

        try {
           int bytesRead = fromServer.read(packet);

           checkBytesRead(bytesRead, SET_UID, "No data received from handshake");

           ByteBuffer bb = ByteBuffer.wrap(packet);

           int[] welcomePayload = new int[bb.get(2)];

           welcomePayload[0] = bb.getInt(3);

           Packet welcomePacket = new Packet((int)bb.get(0), (int)bb.get(1), (int)bb.get(2), welcomePayload);

           player = new GameData(welcomePacket.getPayload()[0]);

           Log.d(SET_UID, "Player UID received from server: " + player.getUid());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getPlayInvitation() {
         final int UPDATE = 20;
         final int CONTEXT = 1;
         final int PAYLOAD_LENGTH = 0;

         byte[] packet = new byte[3];

         try {
             int bytesRead = fromServer.read(packet);

             checkBytesRead(bytesRead, RECEIVE_INVITATION, "Bad invitation received");
         } catch (IOException e) {
             e.printStackTrace();
         }

         if (packet[0] != UPDATE) {
             Log.e(RECEIVE_INVITATION, "Received bad invitation");
             Log.e(RECEIVE_INVITATION, Arrays.toString(packet));
         }

        Log.d(RECEIVE_INVITATION, "Play invitation for player " + player.getUid() + " received successfully " + Arrays.toString(packet));

         //Play buttons should be updated here
    }

    private void sendPlay() {
        final int GAME_ACTION      = 4;
        final int MOVE_MADE        = 2;
        final int PAYLOAD_LENGTH   = 1;

        final int[] payload = { playChoice };

        RequestPacket playReq = new RequestPacket(player.getUid(),
                GAME_ACTION, MOVE_MADE, PAYLOAD_LENGTH,
                payload);

        Log.d(SEND_PLAY, "Play request packet: " + playReq.toString());

        final byte[] playPacket = playReq.toBytes();

        try {
            toServer.write(playPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getPlayAcknowledgement() {
        byte[] playAckPacket = new byte[3];

         try {
             int bytesRead = fromServer.read(playAckPacket);

             checkBytesRead(bytesRead, PLAY_ACK, "No data received in play acknowledgement");
         } catch (IOException e) {
             e.printStackTrace();
         }

        if (playAckPacket[0] == 10) {
            Log.d(PLAY_ACK, "Received acknowledgement from server");
        } else {
            Log.e(PLAY_ACK, "Server sent strange msg. " + Arrays.toString(playAckPacket));
        }
    }

    private void processOutcome() {
        byte[] packetFromServer = new byte[5];

        try {
            int bytesRead = fromServer.read(packetFromServer);
            checkBytesRead(bytesRead, OUTCOME, "No data received in game outcome");
        } catch (IOException e) {
            e.printStackTrace();
        }

        int[] payload = new int[packetFromServer[2]];

        int index = 0;
        for (int i = 3; i < 3 + packetFromServer[2]; i++) {
            payload[index] = packetFromServer[i];
            index++;
        }

        Log.d(STREAM, "Received game outcome " + payload[0]);

        switch (payload[0]) {
            case 1:
                Log.d(OUTCOME, "Win. Opponent played " + payload[1]);
                gameOutcome = Outcomes.WIN.ordinal();
                break;
            case 2:
                Log.d(OUTCOME, "Loss. Opponent played " + payload[1]);
                gameOutcome = Outcomes.LOSS.ordinal();

                break;
            case 3:
                Log.d(OUTCOME, "Tie. Opponent played " + payload[1]);
                gameOutcome = Outcomes.TIE.ordinal();

                break;
            default:
                Log.d(OUTCOME, "ERROR");
                break;
        }


        Log.d(OUTCOME, "Bye");
    }

    private void disableButtons() {
        for (int i = 1; i < buttons.length; i++) {
            buttons[i].setEnabled(false);
        }
    }

    private void enableButtons() {
        for (int i = 1; i < buttons.length; i++) {
            buttons[i].setEnabled(true);
        }
    }

    private void initButtons() {
        connectButton.setEnabled(true);
        disableButtons();
    }

    private void notifyUserOfOutcome() {
        String text;

        switch (gameOutcome){
            case 1:
                text = String.format(getResources().getString(R.string.win), getTextOfPlay(opponentsMove));
                outcome.setText(text);
                break;
            case 2:
                text = String.format(getResources().getString(R.string.loss), getTextOfPlay(opponentsMove));
                outcome.setText(text);
                break;
            case 3:
                text = String.format(getResources().getString(R.string.tie), getTextOfPlay(opponentsMove));
                outcome.setText(text);
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

    private void checkBytesRead(final int bytesRead, final String type, final String message) {
        if (bytesRead < 0) {
            Log.e(type, message);

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                System.exit(1);
            }
        }

        Log.d(type, "Read " + bytesRead + " bytes");
    }

    private void processClick(final int choice, final String choiceString) {
        Log.d(CLICK, "Clicked " + choiceString);

//                sendPlay(choice);
        playChoice = choice;
        notifyUserOfOutcome();
        initButtons();
    }
}