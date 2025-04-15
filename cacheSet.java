import java.util.*;

public class cacheSet {
    
    private int setIndex, numSets, blockSize;
    private List<cache.cacheBlock> blocks = new ArrayList<>(); // Maintained in FIFO order
    private int maxBlocks;
    private List<MemoryBlock> ram;

    public cacheSet(int setIndex, int numSets, int blockSize, int maxBlocks, List<MemoryBlock> ram) {
        this.setIndex = setIndex;
        this.numSets = numSets;
        this.blockSize = blockSize;
        this.maxBlocks = maxBlocks;
        this.ram = ram;
    }

    // Check if an operation with this tag will cause an eviction
    public boolean willCauseEviction(int tag) {
        // Check if the tag is already in the cache (hit)
        for (cache.cacheBlock block : blocks) {
            if (block.filledBlock && block.tag == tag) {
                return false; // Hit, no eviction
            }
        }
        
        // Miss - check if set is full
        return blocks.size() >= maxBlocks;
    }
    
    // Get the block that would be evicted (first/oldest in FIFO)
    public cache.cacheBlock getBlockToEvict() {
        if (blocks.isEmpty()) {
            return null;
        }
        return blocks.get(0); // First block (oldest in FIFO)
    }

    public String store(int tag, int offset, int size, byte[] data, int fullAddress) {
        // Step 1: Check for a hit
        for (cache.cacheBlock block : blocks) {
            if (block.filledBlock && block.tag == tag) {
                // Update data
                for (int i = 0; i < size; i++) {
                    block.data[offset + i] = data[i];
                }
                block.modifiedBlock = true;
                
                return "hit";
            }
        }

        // Step 2: MISS — allocate a new block (write-allocate policy)
        cache.cacheBlock newBlock = new cache.cacheBlock(blockSize);
        newBlock.filledBlock = true;
        newBlock.modifiedBlock = true;
        newBlock.tag = tag;

        // Load the entire block from RAM first
        int ramBlockIndex = fullAddress / blockSize;
        if (ramBlockIndex < ram.size()) {
            System.arraycopy(ram.get(ramBlockIndex).data, 0, newBlock.data, 0, blockSize);
        }

        // Then write the new data
        for (int i = 0; i < size; i++) {
            newBlock.data[offset + i] = data[i];
        }

        // Step 3: Evict if full using FIFO - remove the oldest block (first in the list)
        if (blocks.size() >= maxBlocks) {
            // The first block in the list is the oldest (FIFO)
            blocks.remove(0);
        }

        // Add the new block to the end (newest)
        blocks.add(newBlock);
        return "miss";
    }

    public String load(int tag, int offset, int size, int fullAddress) {
        // Step 1: Check for a hit
        for (cache.cacheBlock block : blocks) {
            if (block.filledBlock && block.tag == tag) {
                return "hit";
            }
        }

        // Step 2: MISS — fetch block from memory
        cache.cacheBlock newBlock = new cache.cacheBlock(blockSize);
        newBlock.filledBlock = true;
        newBlock.modifiedBlock = false;
        newBlock.tag = tag;

        // Load from RAM
        int ramBlockIndex = fullAddress / blockSize;
        if (ramBlockIndex < ram.size()) {
            System.arraycopy(ram.get(ramBlockIndex).data, 0, newBlock.data, 0, blockSize);
        }
           
        // Step 3: Evict if full using FIFO - remove the oldest block (first in the list)
        if (blocks.size() >= maxBlocks) {
            // The first block in the list is the oldest (FIFO)
            blocks.remove(0);
        }

        // Add the new block to the end (newest)
        blocks.add(newBlock);
        return "miss";
    }

    public cache.cacheBlock findBlockByTag(int tag) {
        for (cache.cacheBlock block : blocks) {
            if (block.tag == tag && block.filledBlock) {
                return block;
            }
        }
        return null;
    }
}