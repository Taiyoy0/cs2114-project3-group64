package ticketline;

import java.util.List;
import junit.framework.TestCase;

/** Direct queue tests to meet exceeds on prototype 1
 */
public class QueueTest extends TestCase {
    private Queue<Ticket> queue;

    @Override
    protected void setUp() {
        queue = new Queue<Ticket>();
    }

    private Ticket ticket(String id, String severity) {
        return new Ticket(id, "Software", "Example issue", severity);
    }
    /**
     * tests new queue is empty
     */
    public void testNewQueueIsEmpty() {
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertTrue(queue.toList().isEmpty());
    }
    /**
     * tests adding onto the queue and other tests
     */
    public void testEnqueueAndPeek() {
        Ticket first = ticket("1", "low");
        queue.enqueue(first);
        assertFalse(queue.isEmpty());
        assertEquals(1, queue.size());
        assertSame(first, queue.peek());
        assertSame(first, queue.peek());
        assertEquals(1, queue.size());
    }

    public void testPriorityOrder() {
        queue.enqueue(ticket("1", "low"));
        queue.enqueue(ticket("2", "critical"));
        queue.enqueue(ticket("3", "medium"));
        queue.enqueue(ticket("4", "high"));
        assertEquals("2", queue.dequeue().getId());
        assertEquals("4", queue.dequeue().getId());
        assertEquals("3", queue.dequeue().getId());
        assertEquals("1", queue.dequeue().getId());
        assertTrue(queue.isEmpty());
    }

    public void testEqualPriorityKeepsSubmissionOrder() {
        queue.enqueue(ticket("1", "high"));
        queue.enqueue(ticket("2", "low"));
        queue.enqueue(ticket("3", "high"));
        queue.enqueue(ticket("4", "critical"));
        queue.enqueue(ticket("5", "high"));
        assertEquals("4", queue.dequeue().getId());
        assertEquals("1", queue.dequeue().getId());
        assertEquals("3", queue.dequeue().getId());
        assertEquals("5", queue.dequeue().getId());
        assertEquals("2", queue.dequeue().getId());
    }

    public void testDequeueUpdatesSizeAndNextTicket() {
        Ticket first = ticket("1", "medium");
        Ticket second = ticket("2", "low");
        queue.enqueue(first);
        queue.enqueue(second);
        assertSame(first, queue.dequeue());
        assertEquals(1, queue.size());
        assertSame(second, queue.peek());
        assertSame(second, queue.dequeue());
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
    }

    public void testEnqueueNullDoesNotChangeQueue() {
        Ticket first = ticket("1", "high");
        queue.enqueue(first);
        try {
            queue.enqueue(null);
            fail("Null entries must be rejected.");
        }
        catch (IllegalArgumentException expected) {
            assertEquals(1, queue.size());
            assertSame(first, queue.peek());
        }
    }

    public void testPeekEmptyQueue() {
        try {
            queue.peek();
            fail("Peeking an empty queue must throw an exception.");
        }
        catch (IllegalStateException expected) {
            assertTrue(queue.isEmpty());
            assertEquals(0, queue.size());
        }
    }

    public void testDequeueEmptyQueue() {
        try {
            queue.dequeue();
            fail("Removing from an empty queue must throw an exception.");
        }
        catch (IllegalStateException expected) {
            assertTrue(queue.isEmpty());
            assertEquals(0, queue.size());
        }
    }

    public void testReuseAfterRemovingAllTickets() {
        queue.enqueue(ticket("1", "critical"));
        queue.dequeue();
        Ticket next = ticket("2", "low");
        queue.enqueue(next);
        assertEquals(1, queue.size());
        assertSame(next, queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    public void testToListKeepsOrderAndProtectsMembership() {
        queue.enqueue(ticket("1", "low"));
        queue.enqueue(ticket("2", "critical"));
        List<Ticket> snapshot = queue.toList();
        assertEquals(2, snapshot.size());
        assertEquals("2", snapshot.get(0).getId());
        assertEquals("1", snapshot.get(1).getId());
        snapshot.clear();
        assertEquals(2, queue.size());
        assertEquals("2", queue.peek().getId());
    }
}
