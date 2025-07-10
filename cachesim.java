import java.io.*;
import java.util.*;

public class cachesim {

    static Scanner traceFileScanner;

    // MemoryBlock class
    private static class MemoryBlock {
        byte[] data;

        public MemoryBlock(int blockSize) {
            this.data = new byte[blockSize];
            // if a block has never been written before then its value in main memory is zero
        }
        
        // helper method to get specific bytes from this memory block
        public byte[] getBytes(int offset, int size) {
            byte[] result = new byte[size];
            for (int i = 0; i < size && i + offset < data.length; i++) {
                result[i] = data[offset + i];
            }
            return result;
        }
    }

    // cache block class
    private static class cacheBlock {
        boolean filledBlock;
        boolean modifiedBlock;
        int tag;
        byte[] data;

        public cacheBlock(int blockSize) {
            this.filledBlock = false;
            this.modifiedBlock = false;
            this.tag = -1;
            this.data = new byte[blockSize];
        }
    }

    // cacheSet class
    private static class cacheSet {
        private int setIndex, numSets, blockSize;
        private List<cacheBlock> blocks = new ArrayList<>(); // Maintained in FIFO order
        private int maxBlocks;
        private List<MemoryBlock> ram;

        public cacheSet(int setIndex, int numSets, int blockSize, int maxBlocks, List<MemoryBlock> ram) {
            this.setIndex = setIndex;
            this.numSets = numSets;
            this.blockSize = blockSize;
            this.maxBlocks = maxBlocks;
            this.ram = ram;
        }

        // check if an operation with this tag will cause an eviction
        public boolean willCauseEviction(int tag) {
            // Check if the tag is already in the cache (hit)
            for (cacheBlock block : blocks) {
                if (block.filledBlock && block.tag == tag) {
                    return false; // hit, no eviction
                }
            }
            
            // miss - check if set is full
            return blocks.size() >= maxBlocks;
        }
        
        // get the block that would be evicted (first/oldest in FIFO)
        public cacheBlock getBlockToEvict() {
            if (blocks.isEmpty()) {
                return null;
            }
            return blocks.get(0); // first block (oldest in FIFO)
        }

        public String store(int tag, int offset, int size, byte[] data, int fullAddress) {
            // check for a hit
            for (cacheBlock block : blocks) {
                if (block.filledBlock && block.tag == tag) {
                    // update data
                    for (int i = 0; i < size; i++) {
                        block.data[offset + i] = data[i];
                    }
                    block.modifiedBlock = true;
                    
                    return "hit";
                }
            }

            // miss — allocate a new block (write-allocate)
            cacheBlock newBlock = new cacheBlock(blockSize);
            newBlock.filledBlock = true;
            newBlock.modifiedBlock = true;
            newBlock.tag = tag;

            // load the entire block from RAM first
            int ramBlockIndex = fullAddress / blockSize;
            if (ramBlockIndex < ram.size()) {
                System.arraycopy(ram.get(ramBlockIndex).data, 0, newBlock.data, 0, blockSize);
            }

            // then write the new data
            for (int i = 0; i < size; i++) {
                newBlock.data[offset + i] = data[i];
            }

            // evict if full using FIFO - remove the oldest block (first in the list)
            if (blocks.size() >= maxBlocks) {
                // the first block in the list is the oldest (FIFO)
                blocks.remove(0);
            }

            // add the new block to the end (newest)
            blocks.add(newBlock);
            return "miss";
        }

        public String load(int tag, int offset, int size, int fullAddress) {
            // check for a hit
            for (cacheBlock block : blocks) {
                if (block.filledBlock && block.tag == tag) {
                    return "hit";
                }
            }

            // miss — fetch block from memory
            cacheBlock newBlock = new cacheBlock(blockSize);
            newBlock.filledBlock = true;
            newBlock.modifiedBlock = false;
            newBlock.tag = tag;

            // load from RAM
            int ramBlockIndex = fullAddress / blockSize;
            if (ramBlockIndex < ram.size()) {
                System.arraycopy(ram.get(ramBlockIndex).data, 0, newBlock.data, 0, blockSize);
            }
               
            // evict if full using FIFO - remove the oldest block (first in the list)
            if (blocks.size() >= maxBlocks) {
                // the first block in the list is the oldest (FIFO)
                blocks.remove(0);
            }

            // add the new block to the end (newest)
            blocks.add(newBlock);
            return "miss";
        }

        public cacheBlock findBlockByTag(int tag) {
            for (cacheBlock block : blocks) {
                if (block.tag == tag && block.filledBlock) {
                    return block;
                }
            }
            return null;
        }
    }

    // cache class
    private static class cache {
        private int cacheSize, numOfBlockPerSet, blockSize;
        private int numSets;

        private List<MemoryBlock> RAM = new ArrayList<>();
        private List<cacheSet> sets = new ArrayList<>();
        private byte[] lastLoadedData;

        public cache(int sizeKB, int blocksPerSet, int blockSize) {
            // converts the input values into useful variables
            this.cacheSize = sizeKB * 1024;
            this.numOfBlockPerSet = blocksPerSet;
            this.blockSize = blockSize;

            // calculate number of sets
            this.numSets = cacheSize / (blockSize * numOfBlockPerSet);

            // initialize RAM with zeros - 16MB memory
            for (int i = 0; i < (16 * 1024 * 1024) / blockSize; i++) {
                RAM.add(new MemoryBlock(blockSize));
            }

            // initialize cache sets
            for (int i = 0; i < numSets; i++) {
                sets.add(new cacheSet(i, numSets, blockSize, numOfBlockPerSet, RAM));
            }
        }

        // get set index from address
        private int getSetIndex(int address) {
            return (address / blockSize) & (numSets - 1);
        }

        // get tag from address
        private int getTag(int address) {
            return address / (blockSize * numSets);
        }

        // get offset within block
        private int getOffset(int address) {
            return address & (blockSize - 1);
        }

        public String load(int address, int size) {
            int setIndex = getSetIndex(address);
            int tag = getTag(address);
            int offset = getOffset(address);
            
            // get the set for this address
            cacheSet set = sets.get(setIndex);
            
            // check for potential eviction before accessing
            boolean willEvict = set.willCauseEviction(tag);
            if (willEvict) {
                // handle eviction
                cacheBlock evictedBlock = set.getBlockToEvict();
                int evictedBlockTag = evictedBlock.tag;
                int evictedAddress = (evictedBlockTag * numSets + setIndex) * blockSize;
                
                // print replacement message
                if (evictedBlock.modifiedBlock) {
                    System.out.println("replacement 0x" + Integer.toHexString(evictedAddress) + " dirty");
                    
                    // write back to memory for dirty block
                    MemoryBlock ramBlock = RAM.get(evictedAddress / blockSize);
                    for (int i = 0; i < blockSize; i++) {
                        ramBlock.data[i] = evictedBlock.data[i];
                    }
                } else {
                    System.out.println("replacement 0x" + Integer.toHexString(evictedAddress) + " clean");
                }
            }
            
            // access the cache
            String result = set.load(tag, offset, size, address);
            
            // get the data for this load
            cacheBlock block = set.findBlockByTag(tag);
            if (block != null) {
                lastLoadedData = new byte[size];
                for (int i = 0; i < size; i++) {
                    lastLoadedData[i] = block.data[offset + i];
                }
            } else {
                lastLoadedData = new byte[size]; 
            }
            
            return result;
        }

        public String store(int address, int size, byte[] data) {
            int setIndex = getSetIndex(address);
            int tag = getTag(address);
            int offset = getOffset(address);
            
            // get the set for this address
            cacheSet set = sets.get(setIndex);
            
            // check for potential eviction before accessing
            boolean willEvict = set.willCauseEviction(tag);
            if (willEvict) {
                // handle eviction
                cacheBlock evictedBlock = set.getBlockToEvict();
                int evictedBlockTag = evictedBlock.tag;
                int evictedAddress = (evictedBlockTag * numSets + setIndex) * blockSize;
                
                // print replacement message
                if (evictedBlock.modifiedBlock) {
                    System.out.println("replacement 0x" + Integer.toHexString(evictedAddress) + " dirty");
                    
                    // write back to memory for dirty block
                    MemoryBlock ramBlock = RAM.get(evictedAddress / blockSize);
                    for (int i = 0; i < blockSize; i++) {
                        ramBlock.data[i] = evictedBlock.data[i];
                    }
                } else {
                    System.out.println("replacement 0x" + Integer.toHexString(evictedAddress) + " clean");
                }
            }
            
            // access the cache
            return set.store(tag, offset, size, data, address);
        }

        public byte[] getLoadedData() {
            return lastLoadedData;
        }
    }

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
                // process store operation
                String result = myCache.store(access.address, access.accessSize, access.data);
                System.out.println("store 0x" + Integer.toHexString(access.address) + " " + result);
            } else {
                // process load operation
                String result = myCache.load(access.address, access.accessSize);
                
                // print load address and result
                System.out.print("load 0x" + Integer.toHexString(access.address) + " " + result);
                
                // print loaded data if needed
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
