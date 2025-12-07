package com.logarythm.core.bloom;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

public class BloomFilter {

    private final BitSet bitset;
    private final int bitSize;
    private final int numHashes;

    public BloomFilter(int bitSize, int numHashes) {
        this.bitSize = bitSize;
        this.numHashes = numHashes;
        this.bitset = new BitSet(bitSize);
    }

    public void add(String value) {
        if (value == null)
            return;
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < numHashes; i++) {
            int hash = hash(bytes, i);
            bitset.set(Math.abs(hash % bitSize));
        }
    }

    public boolean mightContain(String value) {
        if (value == null)
            return true;
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < numHashes; i++) {
            int hash = hash(bytes, i);
            if (!bitset.get(Math.abs(hash % bitSize)))
                return false;
        }
        return true;
    }

    private int hash(byte[] data, int seed) {
        int h = seed * 0x5bd1e995;
        for (byte b : data) {
            h = h * 31 + b;
        }
        return h;
    }

    public void saveToFile(File f) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(f))) {
            byte[] bytes = bitset.toByteArray();
            out.writeInt(bytes.length);
            out.write(bytes);
            out.writeInt(bitSize);
            out.writeInt(numHashes);
        }
    }

    public static BloomFilter loadFromFile(File f) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            int byteLen = in.readInt();
            byte[] bytes = new byte[byteLen];
            in.readFully(bytes);

            int bitSize = in.readInt();
            int numHashes = in.readInt();

            BloomFilter bf = new BloomFilter(bitSize, numHashes);
            BitSet bs = BitSet.valueOf(bytes);

            for (int i = 0; i < bitSize; i++) {
                if (bs.get(i))
                    bf.bitset.set(i);
            }
            return bf;
        }
    }
}
