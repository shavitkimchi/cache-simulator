import java.util.*;

public class MemoryBlock {
    byte[] data;

    public MemoryBlock(int blockSize) {
        this.data = new byte[blockSize];
        // If a block has never been written before, then its value in main memory is zero
    }
    
    // Helper method to get specific bytes from this memory block
    public byte[] getBytes(int offset, int size) {
        byte[] result = new byte[size];
        for (int i = 0; i < size && i + offset < data.length; i++) {
            result[i] = data[offset + i];
        }
        return result;
    }
}