import java.util.*;

public class cache {
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

        // Calculate number of sets
        this.numSets = cacheSize / (blockSize * numOfBlockPerSet);

        // Initialize RAM with zeros (as specified in assignment) - 16MB memory
        for (int i = 0; i < (16 * 1024 * 1024) / blockSize; i++) {
            RAM.add(new MemoryBlock(blockSize));
        }

        // Initialize cache sets
        for (int i = 0; i < numSets; i++) {
            sets.add(new cacheSet(i, numSets, blockSize, numOfBlockPerSet, RAM));
        }
    }

    // Get set index from address
    private int getSetIndex(int address) {
        return (address / blockSize) % numSets;
    }

    // Get tag from address
    private int getTag(int address) {
        return address / (blockSize * numSets);
    }

    // Get offset within block
    private int getOffset(int address) {
        return address % blockSize;
    }

    public String load(int address, int size) {
        int setIndex = getSetIndex(address);
        int tag = getTag(address);
        int offset = getOffset(address);
        
        // Get the set for this address
        cacheSet set = sets.get(setIndex);
        
        // Check for potential eviction before accessing
        boolean willEvict = set.willCauseEviction(tag);
        if (willEvict) {
            // Handle eviction
            cache.cacheBlock evictedBlock = set.getBlockToEvict();
            int evictedBlockTag = evictedBlock.tag;
            int evictedAddress = (evictedBlockTag * numSets + setIndex) * blockSize;
            
            // Print replacement message
            if (evictedBlock.modifiedBlock) {
                System.out.println("replacement 0x" + Integer.toHexString(evictedAddress) + " dirty");
                
                // Write back to memory for dirty block
                MemoryBlock ramBlock = RAM.get(evictedAddress / blockSize);
                for (int i = 0; i < blockSize; i++) {
                    ramBlock.data[i] = evictedBlock.data[i];
                }
            } else {
                System.out.println("replacement 0x" + Integer.toHexString(evictedAddress) + " clean");
            }
        }
        
        // Access the cache
        String result = set.load(tag, offset, size, address);
        
        // Get the data for this load
        cache.cacheBlock block = set.findBlockByTag(tag);
        if (block != null) {
            lastLoadedData = new byte[size];
            for (int i = 0; i < size; i++) {
                lastLoadedData[i] = block.data[offset + i];
            }
        } else {
            lastLoadedData = new byte[size]; // Should not happen, but just in case
        }
        
        return result;
    }

    public String store(int address, int size, byte[] data) {
        int setIndex = getSetIndex(address);
        int tag = getTag(address);
        int offset = getOffset(address);
        
        // Get the set for this address
        cacheSet set = sets.get(setIndex);
        
        // Check for potential eviction before accessing
        boolean willEvict = set.willCauseEviction(tag);
        if (willEvict) {
            // Handle eviction
            cache.cacheBlock evictedBlock = set.getBlockToEvict();
            int evictedBlockTag = evictedBlock.tag;
            int evictedAddress = (evictedBlockTag * numSets + setIndex) * blockSize;
            
            // Print replacement message
            if (evictedBlock.modifiedBlock) {
                System.out.println("replacement 0x" + Integer.toHexString(evictedAddress) + " dirty");
                
                // Write back to memory for dirty block
                MemoryBlock ramBlock = RAM.get(evictedAddress / blockSize);
                for (int i = 0; i < blockSize; i++) {
                    ramBlock.data[i] = evictedBlock.data[i];
                }
            } else {
                System.out.println("replacement 0x" + Integer.toHexString(evictedAddress) + " clean");
            }
        }
        
        // Access the cache
        return set.store(tag, offset, size, data, address);
    }

    public byte[] getLoadedData() {
        return lastLoadedData;
    }

    // Inner class for cache blocks
    public static class cacheBlock {
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
}