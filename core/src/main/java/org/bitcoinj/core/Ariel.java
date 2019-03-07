package org.bitcoinj.core;

import org.bitcoinj.core.peerdetector.BitcoinPeerDetector;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Ariel {
    public static void main(String[] args) throws IOException {
        NetworkParameters params = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);

        BitcoinPeerDetector detector = new BitcoinPeerDetector(params);

        boolean isBitcoinNode = detector.checkPeerBlocking(new InetSocketAddress("127.0.0.1", 18444));

        System.out.printf("Is node: %s", isBitcoinNode);
    }
}
