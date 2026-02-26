package app.falcon.siv.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChannelMemoryTest {

    @Test
    void addMessageAndToPrompt() {
        ChannelMemory memory = new ChannelMemory(3);
        memory.add("Hello world");
        assertEquals(1, memory.size());
        assertTrue(memory.toPrompt().contains("Hello world"));
    }

    @Test
    void slidingWindowEvictsOldestWhenFull() {
        ChannelMemory memory = new ChannelMemory(3);
        memory.add("msg1");
        memory.add("msg2");
        memory.add("msg3");
        memory.add("msg4"); // should evict msg1

        assertEquals(3, memory.size());
        String prompt = memory.toPrompt();
        assertFalse(prompt.contains("msg1"), "Oldest message should have been evicted");
        assertTrue(prompt.contains("msg2"));
        assertTrue(prompt.contains("msg3"));
        assertTrue(prompt.contains("msg4"));
    }

    @Test
    void emptyMemoryProducesEmptyPrompt() {
        ChannelMemory memory = new ChannelMemory(10);
        assertEquals("", memory.toPrompt());
    }

    @Test
    void sizeNeverExceedsMaxSize() {
        ChannelMemory memory = new ChannelMemory(5);
        for (int i = 0; i < 100; i++) {
            memory.add("message " + i);
        }
        assertEquals(5, memory.size());
    }
}
