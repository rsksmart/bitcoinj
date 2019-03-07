package org.bitcoinj.core.peerdetector;

import org.bitcoinj.core.*;
import org.bitcoinj.net.MessageWriteTarget;
import org.bitcoinj.net.StreamConnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Implements the logic to perform a partial handshake with a bitcoin peer
 * on the p2p protocol.
 *
 * As per https://en.bitcoin.it/wiki/Version_Handshake, the handshake
 * process is as follows (L = local, R = remote):
 *
 * L -> R: Send version message with the local peer's version
 * R -> L: Send version message back
 * R -> L: Send verack message (*)
 * R:      Sets version to the minimum of the 2 versions
 * L -> R: Send verack message after receiving version message from R
 * L:      Sets version to the minimum of the 2 versions
 *
 * Here we only get to the (*) stage, since we're only
 * interested in verifying with a high probability that the peer
 * is actually a bitcoin node.
 */
public class Handshaker implements StreamConnection {
    public enum State {
        NOT_SENT,
        SENT_VERSION,
        RECEIVED_VERSION,
        RECEIVED_VERACK,
        UNEXPECTED,
        DISCONNECTED
    }

    private NetworkParameters params;
    private MessageWriteTarget writeTarget;
    private BitcoinSerializer serializer;
    private State state;


    public Handshaker(NetworkParameters params) {
        this.params = params;
        this.serializer = new BitcoinSerializer(params, false);
        this.state = State.NOT_SENT;
    }

    @Override
    public void connectionOpened() {
        // Do the handshake
        VersionMessage versionMessage = new VersionMessage(params, 0);
        versionMessage.relayTxesBeforeFilter = false;
        versionMessage.time = Utils.currentTimeSeconds();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            serializer.serialize(versionMessage, out);
            writeTarget.writeBytes(out.toByteArray());
            this.state = State.SENT_VERSION;
        } catch (IOException e) {
            System.out.println(String.format("Exception: %s", e.getMessage()));
        }
    }

    @Override
    public int receiveBytes(ByteBuffer buff) throws Exception {
        boolean gotMessage;
        int position = buff.position();

        do {
            try {
                position = buff.position();
                Message message = serializer.deserialize(buff);
                processMessage(message);
                gotMessage = true;
            } catch (Exception e) {
                gotMessage = false;
            }
        } while(gotMessage);

        return position;
    }

    private void processMessage(Message message) {
        switch (this.state) {
            case SENT_VERSION:
                if (message.getClass() == VersionMessage.class) {
                    this.state = State.RECEIVED_VERSION;
                }
                break;
            case RECEIVED_VERSION:
                if (message.getClass() == VersionAck.class) {
                    this.state = State.RECEIVED_VERACK;
                }
                break;
            case RECEIVED_VERACK:
                break;
            default:
                this.state = State.UNEXPECTED;
        }
    }

    @Override
    public void connectionClosed() {
        writeTarget.closeConnection();
        this.state = State.DISCONNECTED;
    }

    @Override
    public void setWriteTarget(MessageWriteTarget writeTarget) {
        this.writeTarget = writeTarget;
    }

    @Override
    public int getMaxMessageSize() {
        return 0;
    }

    public State getState() {
        return state;
    }
}
