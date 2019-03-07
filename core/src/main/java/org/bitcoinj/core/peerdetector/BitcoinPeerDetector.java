package org.bitcoinj.core.peerdetector;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.net.BlockingClient;

import javax.net.SocketFactory;
import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class BitcoinPeerDetector {
    private NetworkParameters params;
    private int timeout;
    private int pollingFrequency;

    private static final int DEFAULT_TIMEOUT = 5000;
    private static final int DEFAULT_POLLING_FREQUENCY = 100;

    public BitcoinPeerDetector(NetworkParameters params) {
        this.params = params;
        this.timeout = DEFAULT_TIMEOUT;
        this.pollingFrequency = DEFAULT_POLLING_FREQUENCY;

        // Needed to use bitcoinj
        Context.getOrCreate(params);
    }

    public boolean checkPeerBlocking(SocketAddress peerAddress) {
        try {
            return getTask(peerAddress).call();
        } catch (Exception e) {
            return false;
        }
    }

    private Callable<Boolean> getTask(SocketAddress peerAddress) {
        return () -> {
            Handshaker handshaker = new Handshaker(params);

            BlockingClient client = new BlockingClient(
                    peerAddress,
                    handshaker, this.timeout,
                    SocketFactory.getDefault(), null
            );

            while (true) {
                try {
                    // Error connecting/reading?
                    if (client.getConnectFuture().isDone()) {
                        try {
                            client.getConnectFuture().get();
                        } catch (ExecutionException e) {
                            return false;
                        }
                    }

                    switch (handshaker.getState()) {
                        case RECEIVED_VERACK:
                            return true;
                        case UNEXPECTED:
                        case DISCONNECTED:
                            return false;
                        default:
                    }

                    Thread.sleep(this.pollingFrequency);
                } catch (Exception e) {
                    return false;
                }
            }
        };
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getPollingFrequency() {
        return pollingFrequency;
    }

    public void setPollingFrequency(int pollingFrequency) {
        this.pollingFrequency = pollingFrequency;
    }
}
