package org.bitcoinj.tools;

import static org.bitcoinj.core.CheckpointManager.BASE64;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import org.bitcoinj.core.CheckpointManager;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.StoredBlock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BuildCheckpointsTest {

    private static final NetworkParameters MAINNET = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);

    private static final String TEXT_FORMAT_PREFIX = "TXT CHECKPOINTS 1";
    private static final String DEFAULT_NUM_OF_SIGNATURES_VALUE = "0";

    private static final List<String> CHECKPOINTS_12_BYTES_CHAINWORK_ENCODED = Arrays.asList(
        "AAAAAAAAB+EH4QfhAAAH4AEAAAApmwX6UCEnJcYIKTa7HO3pFkqqNhAzJVBMdEuGAAAAAPSAvVCBUypCbBW/OqU0oIF7ISF84h2spOqHrFCWN9Zw6r6/T///AB0E5oOO",
        "AAAAAAAAD8QPxA/EAAAPwAEAAADHtJ8Nq3z30grJ9lTH6bLhKSHX+MxmkZn8z5wuAAAAAK0gXcQFtYSj/IB2KZ38+itS1Da0Dn/3XosOFJntz7A8OsC/T8D/Pxwf0no+",
        "AAAAAAAALUAtQC1AAAAXoAEAAABwvpBfmfp76xvcOzhdR+OPnJ2aLD5znGpD8LkJAAAAALkv0fxOJYZ1dMLCyDV+3AB0y+BW8lP5/8xBMMqLbX7u+gPDT/D/DxwDvhrh"
    );

    private static final List<String> CHECKPOINTS_32_BYTES_CHAINWORK_ENCODED = Arrays.asList(
        // 13 bytes TOO_LARGE_WORK_V1 = ffffffffffffffffffffffffff
        "AAAAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAQAAACmbBfpQISclxggpNrsc7ekWSqo2EDMlUEx0S4YAAAAA9IC9UIFTKkJsFb86pTSggXshIXziHayk6oesUJY31nDqvr9P//8AHQTmg44",
        "AAAAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAQAAAMe0nw2rfPfSCsn2VMfpsuEpIdf4zGaRmfzPnC4AAAAArSBdxAW1hKP8gHYpnfz6K1LUNrQOf/deiw4Ume3PsDw6wL9PwP8/HB/Sej4",
        "AAAAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAQAAAHC+kF+Z+nvrG9w7OF1H44+cnZosPnOcakPwuQkAAAAAuS/R/E4lhnV0wsLINX7cAHTL4FbyU/n/zEEwyottfu76A8NP8P8PHAO+GuE",
        // 32 bytes MAX_WORK_V2 = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        "//////////////////////////////////////////8AAB+AAQAAANW+iGqrr/fsekjWfL7yhyKCGSieKwRG8nmcnAoAAAAAFEW4aog6zdt5sMVmp3UMo/H/JkXiG/u3vmsfyYvo5ThKBcNPwP8/HBEzbVs",
        "//////////////////////////////////////////8AACdgAQAAAAnfZFAmRFbc2clq5XzNV2/UbKPLCAB7JOECcDoAAAAAeCpL87HF9/JFao8VX1rqRU/pMsv8F08X8ieq464NqECaBsNP//8AHRvpMAo",
        "//////////////////////////////////////////8AAC9AAQAAAMipH0cUa3D2Ea/T7sCMt0G4Tuqq5/b/KugBHgYAAAAAIROhXYS8rkGyrLjTJvp2iWRfTDOcu/Rkkf9Az5xpTLjrB8NPwP8/HGbjgbo"
    );

    private TreeMap<Integer, StoredBlock> checkpoints;
    private File textFile;

    @Before
    public void setUp() throws Exception {
        checkpoints = new TreeMap<>();
        textFile = File.createTempFile("checkpoints", ".txt");
        textFile.delete();
    }

    @After
    public void tearDown() {
        textFile.delete();
    }

    @Test
    public void writeTextualCheckpoints_whenBlocksChainWorkFits12Bytes_shouldBuiltFile() throws IOException {
        assertFalse(textFile.exists());
        populateCheckpoints(CHECKPOINTS_12_BYTES_CHAINWORK_ENCODED, StoredBlock.COMPACT_SERIALIZED_SIZE_LEGACY, MAINNET);
        BuildCheckpoints.writeTextualCheckpoints(checkpoints, textFile);
        assertTrue(textFile.exists());

        assertCheckpointFileContent(CHECKPOINTS_12_BYTES_CHAINWORK_ENCODED);

        try (FileInputStream checkpointsStream = new FileInputStream(textFile)) {
            CheckpointManager checkpointManager = new CheckpointManager(MAINNET, checkpointsStream);
            assertEquals(checkpoints.size(), checkpointManager.numCheckpoints());
        } catch (Exception ex) {
            fail();
        }
    }

    private void assertCheckpointFileContent(List<String> checkpoints12BytesChainworkEncoded) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(textFile))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
        } catch (Exception e) {
            fail();
        }

        assertEquals(TEXT_FORMAT_PREFIX, lines.get(0));
        assertEquals(DEFAULT_NUM_OF_SIGNATURES_VALUE, lines.get(1));
        assertEquals(checkpoints.size(), Integer.parseInt(lines.get(2)));
        for (int index = 3; index < lines.size(); index++) {
            assertTrue(checkpoints12BytesChainworkEncoded.contains(lines.get(index)));
        }
    }

    private void populateCheckpoints(List<String> encodedCheckpoints, int blockFormatSize,
        NetworkParameters networkParameters) {
        List<StoredBlock> decodedCheckpoints = getCheckpoints(encodedCheckpoints, blockFormatSize,
            networkParameters);
        for (int i = 0; i < decodedCheckpoints.size(); i++) {
            checkpoints.put(i, decodedCheckpoints.get(i));
        }
    }

    private List<StoredBlock> getCheckpoints(List<String> encodedCheckpoints, int blockFormatSize,
        NetworkParameters networkParameters) {
        ByteBuffer buffer = ByteBuffer.allocate(blockFormatSize);
        List<StoredBlock> decodedCheckpoints = new ArrayList<StoredBlock>();
        for (String checkpoint : encodedCheckpoints) {
            byte[] bytes = BASE64.decode(checkpoint);
            buffer.clear();
            buffer.put(bytes);
            buffer.flip();
            StoredBlock block;
            if (blockFormatSize == StoredBlock.COMPACT_SERIALIZED_SIZE_LEGACY) {
                block = StoredBlock.deserializeCompactLegacy(networkParameters, buffer);
            } else {
                block = StoredBlock.deserializeCompactV2(networkParameters, buffer);
            }
            decodedCheckpoints.add(block);
        }
        return decodedCheckpoints;
    }

    @Test
    public void writeTextualCheckpoints_whenBlocksChainWorkUse32Bytes_shouldBuiltFile() throws IOException {
        assertFalse(textFile.exists());
        populateCheckpoints(CHECKPOINTS_32_BYTES_CHAINWORK_ENCODED, StoredBlock.COMPACT_SERIALIZED_SIZE_V2, MAINNET);
        BuildCheckpoints.writeTextualCheckpoints(checkpoints, textFile);
        assertTrue(textFile.exists());

        assertCheckpointFileContent(CHECKPOINTS_32_BYTES_CHAINWORK_ENCODED);

        try (FileInputStream checkpointsStream = new FileInputStream(textFile)) {
            CheckpointManager checkpointManager = new CheckpointManager(MAINNET, checkpointsStream);
            assertEquals(checkpoints.size(), checkpointManager.numCheckpoints());
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void writeTextualCheckpoints_whenMixBlocksChainWork_shouldBuiltFile() throws IOException {
        assertFalse(textFile.exists());
        populateCheckpoints(CHECKPOINTS_12_BYTES_CHAINWORK_ENCODED, StoredBlock.COMPACT_SERIALIZED_SIZE_LEGACY, MAINNET);
        populateCheckpoints(CHECKPOINTS_32_BYTES_CHAINWORK_ENCODED, StoredBlock.COMPACT_SERIALIZED_SIZE_V2, MAINNET);
        BuildCheckpoints.writeTextualCheckpoints(checkpoints, textFile);
        assertTrue(textFile.exists());

        List<String> expectedEncodedCheckpoints = new ArrayList<>();
        expectedEncodedCheckpoints.addAll(CHECKPOINTS_12_BYTES_CHAINWORK_ENCODED);
        expectedEncodedCheckpoints.addAll(CHECKPOINTS_32_BYTES_CHAINWORK_ENCODED);
        assertCheckpointFileContent(expectedEncodedCheckpoints);

        try (FileInputStream checkpointsStream = new FileInputStream(textFile)) {
            CheckpointManager checkpointManager = new CheckpointManager(MAINNET, checkpointsStream);
            assertEquals(checkpoints.size(), checkpointManager.numCheckpoints());
        } catch (Exception ex) {
            fail();
        }
    }
}