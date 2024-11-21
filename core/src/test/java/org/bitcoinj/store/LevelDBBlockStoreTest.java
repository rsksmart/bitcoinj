/*
 * Copyright by the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.store;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Utils;
import org.junit.*;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class LevelDBBlockStoreTest {
    private static final NetworkParameters MAINNET = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);

    // Max chain work to fit in 12 bytes
    private static final BigInteger MAX_WORK_V1 = new BigInteger(/* 12 bytes */ "ffffffffffffffffffffffff", 16);
    // Chain work too large to fit in 12 bytes
    private static final BigInteger TOO_LARGE_WORK_V1 = new BigInteger(/* 13 bytes */ "ffffffffffffffffffffffffff", 16);
    // Max chain work to fit in 32 bytes
    private static final BigInteger MAX_WORK_V2 = new BigInteger(/* 32 bytes */
        "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
    // Chain work too large to fit in 32 bytes
    private static final BigInteger TOO_LARGE_WORK_V2 = new BigInteger(/* 33 bytes */
        "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);

    private static final List<String> storedBlockHeadersInHex = Arrays.asList(
        // 775000
        "00a0c22d51e2a4331e6d0df54e138f64f78a48ac2c3df3e42d43030000000000000000003a9b781c4034edda9bf6cdc4c15fe1b89c8371a65d0da2536fd7ac73bade67ac2e5ade632027071734409fbf",
        // 775001
        "00e0b827d1206fed3dd95dfe26b2ac2dd986c5be5287247ac98101000000000000000000e188f54eaf56db85c22917f482f0918ed201cc6ba0da4018a6d4be1d43648b11bf5bde632027071778d866ec",
        // 775002
        "00e00020025ab75d6fd1d6eb279e71874b98de591984153e134c05000000000000000000bb7e5313234ba04a48e7c327abd03efbde0d65a37ffad9e72b3e350ca54d97cf665fde6320270717f98d5f43"
    );

    private static final List<String> blockHeadersInHex = Arrays.asList(
        // 775003
        "000000201ccf1f76ab93ea52e22e753f1b3a040c498434141d8a020000000000000000003483c0701c22fbe12e335888ecebf204742d2821c624f0aef02e412d334b9caf9a5fde6320270717edcb6a1b",
        // 775004
        "000080209575aaeb1f5a2eacc45d96adf3f58f7e77cc3c3b0e8000000000000000000000e4d3d5a59b650595a1b3fc1341c61db52f7bc61e2ed06ee3b0061f0860c085603960de63202707171797a32b",
        // 775005
        "0000652ca44cdbb08c25b45f02e3efd835dc74738807093ab8b80600000000000000000087df28e93fe1ecc6259fe912975af6e8b5e4385385c6c08e51bc0eff42e1886f7261de632027071724d2eab5"
    );

    private static final BitcoinSerializer bitcoinSerializer = new BitcoinSerializer(MAINNET, false);

    @Test
    public void testLevelDbBlockStore_whenDbIsNew_shouldWork() throws Exception {
        List<Block> existingBlockHeaders = getBlockHeaders(storedBlockHeadersInHex);

        File levelDbBlockStore = File.createTempFile("leveldb", null);
        levelDbBlockStore.delete();

        Context context = new Context(MAINNET);
        LevelDBBlockStore store = new LevelDBBlockStore(context, levelDbBlockStore);
        store.reset();

        Block block1 = existingBlockHeaders.get(0);
        StoredBlock expectedBlock1 = new StoredBlock(block1.cloneAsHeader(), BigInteger.ZERO, 750000);
        store.put(expectedBlock1);

        Block block2 = existingBlockHeaders.get(1);
        StoredBlock expectedBlock2 = new StoredBlock(block2.cloneAsHeader(), BigInteger.ONE, 750001);
        store.put(expectedBlock2);

        Block block3 = existingBlockHeaders.get(2);
        StoredBlock expectedBlock3 = new StoredBlock(block3.cloneAsHeader(), MAX_WORK_V1, 750002);
        store.put(expectedBlock3);

        store.setChainHead(expectedBlock3);
        store.close();

        store = new LevelDBBlockStore(context, levelDbBlockStore);
        try {

            StoredBlock actualBlock1 = store.get(block1.getHash());
            assertEquals(expectedBlock1, actualBlock1);

            StoredBlock actualBlock2 = store.get(block2.getHash());
            assertEquals(expectedBlock2, actualBlock2);

            StoredBlock actualBlock3 = store.get(block3.getHash());
            assertEquals(expectedBlock3, actualBlock3);

            StoredBlock actualChainHead = store.getChainHead();
            assertEquals(expectedBlock3, actualChainHead);
        } finally {
            store.close();
            store.destroy();
        }
    }

    @Test
    public void testLevelDbBlockStore_whenDbWasCreatedUsingLegacyFormat_shouldWork() throws Exception {
        List<Block> existingBlockHeaders = getBlockHeaders(storedBlockHeadersInHex);

        String levelDbBlockStorePath = getClass().getResource("/leveldb-using-legacy-format").getPath();
        File levelDbBlockStore = new File(levelDbBlockStorePath);
        Context context = new Context(MAINNET);
        LevelDBBlockStore store = new LevelDBBlockStore(context, levelDbBlockStore);
        try {
            Block block1 = existingBlockHeaders.get(0);
            StoredBlock expectedBlock1 = new StoredBlock(block1.cloneAsHeader(), BigInteger.ZERO, 750000);
            StoredBlock actualBlock1 = store.get(block1.getHash());
            assertEquals(expectedBlock1, actualBlock1);

            Block block2 = existingBlockHeaders.get(1);
            StoredBlock expectedBlock2 = new StoredBlock(block2.cloneAsHeader(), BigInteger.ONE, 750001);
            StoredBlock actualBlock2 = store.get(block2.getHash());
            assertEquals(expectedBlock2, actualBlock2);

            Block block3 = existingBlockHeaders.get(2);
            StoredBlock expectedBlock3 = new StoredBlock(block3.cloneAsHeader(), MAX_WORK_V1, 750002);
            StoredBlock actualBlock3 = store.get(block3.getHash());
            assertEquals(expectedBlock3, actualBlock3);

            StoredBlock actualChainHead = store.getChainHead();
            assertEquals(expectedBlock3, actualChainHead);
        } finally {
            store.close();
        }
    }

    @Test
    public void testLevelDbBlockStore_whenAddingV2FormatBlocksToExistingDbCreatedUsingLegacyFormat_shouldWork() throws Exception {
        List<Block> existingBlockHeaders = getBlockHeaders(storedBlockHeadersInHex);

        String levelDbBlockStorePath = getClass().getResource(
            "/leveldb-using-legacy-format-to-add-v2").getPath();
        File levelDbBlockStore = new File(levelDbBlockStorePath);
        Context context = new Context(MAINNET);

        LevelDBBlockStore store = new LevelDBBlockStore(context, levelDbBlockStore);

        List<Block> blockHeadersToAdd = getBlockHeaders(blockHeadersInHex);

        // Add blocks with V2 format
        Block block4 = blockHeadersToAdd.get(0);
        StoredBlock expectedBlock4 = new StoredBlock(block4.cloneAsHeader(), TOO_LARGE_WORK_V1, 750003);
        store.put(expectedBlock4);

        Block block5 = blockHeadersToAdd.get(1);
        StoredBlock expectedBlock5 = new StoredBlock(block5.cloneAsHeader(), MAX_WORK_V2, 750004);
        store.put(expectedBlock5);

        Block block6 = blockHeadersToAdd.get(2);
        StoredBlock expectedBlock6 = new StoredBlock(block6.cloneAsHeader(), MAX_WORK_V2, 750005);
        store.put(expectedBlock6);

        // Set new chain head
        store.setChainHead(expectedBlock6);
        store.close();

        store = new LevelDBBlockStore(context, levelDbBlockStore);
        try {
            Block block1 = existingBlockHeaders.get(0);
            StoredBlock expectedBlock1 = new StoredBlock(block1.cloneAsHeader(), BigInteger.ZERO, 750000);
            StoredBlock actualBlock1 = store.get(block1.getHash());
            assertEquals(expectedBlock1, actualBlock1);

            Block block2 = existingBlockHeaders.get(1);
            StoredBlock expectedBlock2 = new StoredBlock(block2.cloneAsHeader(), BigInteger.ONE, 750001);
            StoredBlock actualBlock2 = store.get(block2.getHash());
            assertEquals(expectedBlock2, actualBlock2);

            Block block3 = existingBlockHeaders.get(2);
            StoredBlock expectedBlock3 = new StoredBlock(block3.cloneAsHeader(), MAX_WORK_V1, 750002);
            StoredBlock actualBlock3 = store.get(block3.getHash());
            assertEquals(expectedBlock3, actualBlock3);

            StoredBlock actualBlock4 = store.get(block4.getHash());
            assertEquals(expectedBlock4, actualBlock4);

            StoredBlock actualBlock5 = store.get(block5.getHash());
            assertEquals(expectedBlock5, actualBlock5);

            StoredBlock actualBlock6 = store.get(block6.getHash());
            assertEquals(expectedBlock6, actualBlock6);

            StoredBlock actualChainHead = store.getChainHead();
            assertEquals(expectedBlock6, actualChainHead);
        } finally {
            store.close();
        }
    }

    @Test
    public void testLevelDbBlockStore_whenAddingMixFormatBlocksAndDbIsNew_shouldWork() throws Exception {
        List<Block> existingBlockHeaders = getBlockHeaders(storedBlockHeadersInHex);

        File levelDbBlockStore = File.createTempFile("leveldb", null);
        levelDbBlockStore.delete();

        Context context = new Context(MAINNET);
        LevelDBBlockStore store = new LevelDBBlockStore(context, levelDbBlockStore);
        store.reset();

        Block block1 = existingBlockHeaders.get(0);
        StoredBlock expectedBlock1 = new StoredBlock(block1.cloneAsHeader(), BigInteger.ZERO, 750000);
        store.put(expectedBlock1);

        Block block2 = existingBlockHeaders.get(1);
        StoredBlock expectedBlock2 = new StoredBlock(block2.cloneAsHeader(), BigInteger.ONE, 750001);
        store.put(expectedBlock2);

        Block block3 = existingBlockHeaders.get(2);
        StoredBlock expectedBlock3 = new StoredBlock(block3.cloneAsHeader(), MAX_WORK_V1, 750002);
        store.put(expectedBlock3);

        // Add blocks with V2 format
        List<Block> blockHeadersToAdd = getBlockHeaders(blockHeadersInHex);

        Block block4 = blockHeadersToAdd.get(0);
        StoredBlock expectedBlock4 = new StoredBlock(block4.cloneAsHeader(), TOO_LARGE_WORK_V1, 750003);
        store.put(expectedBlock4);

        Block block5 = blockHeadersToAdd.get(1);
        StoredBlock expectedBlock5 = new StoredBlock(block5.cloneAsHeader(), MAX_WORK_V2, 750004);
        store.put(expectedBlock5);

        Block block6 = blockHeadersToAdd.get(2);
        StoredBlock expectedBlock6 = new StoredBlock(block6.cloneAsHeader(), MAX_WORK_V2, 750005);
        store.put(expectedBlock6);

        store.setChainHead(expectedBlock6);
        store.close();

        store = new LevelDBBlockStore(context, levelDbBlockStore);
        try {

            StoredBlock actualBlock1 = store.get(block1.getHash());
            assertEquals(expectedBlock1, actualBlock1);

            StoredBlock actualBlock2 = store.get(block2.getHash());
            assertEquals(expectedBlock2, actualBlock2);

            StoredBlock actualBlock3 = store.get(block3.getHash());
            assertEquals(expectedBlock3, actualBlock3);

            StoredBlock actualBlock4 = store.get(block4.getHash());
            assertEquals(expectedBlock4, actualBlock4);

            StoredBlock actualBlock5 = store.get(block5.getHash());
            assertEquals(expectedBlock5, actualBlock5);

            StoredBlock actualBlock6 = store.get(block6.getHash());
            assertEquals(expectedBlock6, actualBlock6);

            StoredBlock actualChainHead = store.getChainHead();
            assertEquals(expectedBlock6, actualChainHead);
        } finally {
            store.close();
            store.destroy();
        }
    }

    private static List<Block> getBlockHeaders(List<String> blockHeadersInHex) {
        List<Block> blocks = new ArrayList<>();
        for (String blockHeader : blockHeadersInHex) {
            blocks.add(bitcoinSerializer.makeBlock(Utils.HEX.decode(blockHeader)));
        }
        return blocks;
    }
}