package app.falcon.siv.ai;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Thread-safe sliding window buffer of the last N messages for a given
 * channel/DID.
 * Used by {@link AiContextService} to give the LLM enough rolling context
 * without unbounded memory growth.
 */
public class ChannelMemory {

    private final int maxSize;
    private final Deque<String> messages;

    public ChannelMemory(int maxSize) {
        this.maxSize = maxSize;
        this.messages = new ArrayDeque<>(maxSize);
    }

    /**
     * Adds a message. If the buffer is full, evicts the oldest message first.
     */
    public synchronized void add(String message) {
        if (messages.size() >= maxSize) {
            messages.pollFirst();
        }
        messages.addLast(message);
    }

    /**
     * Produces a single prompt string from the rolling window, ready to send to the
     * LLM.
     */
    public synchronized String toPrompt() {
        return String.join("\n", messages);
    }

    public synchronized int size() {
        return messages.size();
    }
}
