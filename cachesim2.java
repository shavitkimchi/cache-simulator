import java.io.*;
import java.util.*;

public class cachesim2 {

    static Scanner traceFileScanner;

    // Struct describing an access from the trace file. Returned by `traceNextAccess`.
    private static class CacheAccess {

        boolean isStore;
        int address;
        int accessSize;
        byte data[];
    }

    /**
     * Opens a trace file, given its name. Must be called before `traceNextAccess`,
     * which will begin reading from this file.
     * @param filename: the name of the trace file to open
     */
    public static void traceInit(String filename) {
        try {
            traceFileScanner = new Scanner(new File(filename));
        } catch (FileNotFoundException e) {
            System.err.println("Failed to open trace file: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Checks if we've already read all accesses from the trace file.
     * @return true if the trace file is complete, false if there's more to read.
     */
    public static boolean traceFinished() {
        return !traceFileScanner.hasNextLine();
    }

    /**
     * Read the next access in the trace. Errors if `traceFinished() == true`.
     * @return The access as a `cacheAccess` struct.
     */
    public static CacheAccess traceNextAccess() {
        String[] parts = traceFileScanner.nextLine().strip().split("\\s+");
        CacheAccess result = new CacheAccess();

        // Parse address and access size
        result.address = Integer.parseInt(parts[1].substring(2), 16);
        result.accessSize = Integer.parseInt(parts[2]);

        // Check access type
        if (parts[0].equals("store")) {
            result.isStore = true;

            // Read data
            result.data = new byte[result.accessSize];
            for (int i = 0; i < result.accessSize; i++) {
                result.data[i] = (byte) Integer.parseInt(
                    parts[3].substring(i * 2, 2 + i * 2),
                    16
                );
            }
        } else if (parts[0].equals("load")) {
            result.isStore = false;
        } else {
            System.err.println("Invalid trace file access type" + parts[0]);
            System.exit(1);
        }
        return result;
    }
    // Your code can go here (or anywhere in this class), including a `public static void main` method.

    
    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: java CacheSim <tracefile> <cache-size-kB> <num-of-blocks-per-set> <block-size>");
            System.exit(1);
        }

        String traceFile = args[0];
        int cacheSizeKB = Integer.parseInt(args[1]);
        int numOfBlockPerSet = Integer.parseInt(args[2]); 
        int blockSize = Integer.parseInt(args[3]);

        traceInit(traceFile); // open the trace file and prepare it for reading
        
        cache myCache = new cache(cacheSizeKB, numOfBlockPerSet, blockSize);
       
        while (!traceFinished()) {
            CacheAccess access = traceNextAccess();
            
            if (access.isStore) {
                // Process store operation
                String result = myCache.store(access.address, access.accessSize, access.data);
                System.out.println("store 0x" + Integer.toHexString(access.address) + " " + result);
            } else {
                // Process load operation
                String result = myCache.load(access.address, access.accessSize);
                
                // Print load address and result
                System.out.print("load 0x" + Integer.toHexString(access.address) + " " + result);
                
                // Print loaded data if needed
                byte[] data = myCache.getLoadedData();
                if (data != null) {
                    StringBuilder dataHex = new StringBuilder();
                    for (int i = 0; i < data.length; i++) {
                        dataHex.append(String.format("%02x", data[i] & 0xFF));
                    }
                    System.out.print(" " + dataHex.toString());
                }
                System.out.println();
            }
        }

        System.exit(0);
    }
}
