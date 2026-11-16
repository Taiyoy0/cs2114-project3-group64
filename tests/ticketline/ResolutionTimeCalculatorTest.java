package ticketline;

import java.util.Arrays;
import java.util.Collections;
import junit.framework.TestCase;

/** Deterministic model tests: no view, keyboard, real waiting, or file writes. */
public class ResolutionTimeCalculatorTest extends TestCase {
    private ResolutionTimeCalculator calculator;
    private Queue<Ticket> pending;

    @Override
    protected void setUp() {
        calculator = new ResolutionTimeCalculator();
        pending = new Queue<Ticket>();
        pending.enqueue(new Ticket("1", "Software", "Low ticket", "low"));
        pending.enqueue(new Ticket("2", "Network", "First critical", "critical"));
        pending.enqueue(new Ticket("3", "Hardware", "Second critical", "critical"));
    }

    private void expectInvalid(Runnable action) {
        try {
            action.run();
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            // Expected rejection; individual tests also check unchanged state.
        }
    }

    public void testNoSamples() {
        assertEquals(-1L, calculator.getAverageResolutionTime());
        assertEquals(-1L, calculator.getEstimatedWaitTime(pending.toList(), "2", null));
    }

    public void testAverageAndRounding() {
        calculator.recordResolutionTime(60);
        calculator.recordResolutionTime(121);
        assertEquals(90L, calculator.getAverageResolutionTime());
    }

    public void testZeroDurationIsValidData() {
        calculator.recordResolutionTime(0);
        assertEquals(0L, calculator.getAverageResolutionTime());
        assertEquals(0L, calculator.getEstimatedWaitTime(pending.toList(), "1", null));
    }

    public void testNegativeSampleDoesNotChangeAverage() {
        calculator.recordResolutionTime(60);
        expectInvalid(() -> calculator.recordResolutionTime(-1));
        assertEquals(60L, calculator.getAverageResolutionTime());
    }

    public void testLargeSamplesDoNotOverflow() {
        calculator.recordResolutionTime(Long.MAX_VALUE);
        calculator.recordResolutionTime(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, calculator.getAverageResolutionTime());
        assertEquals(Long.MAX_VALUE,
            calculator.getEstimatedWaitTime(pending.toList(), "1", 0L));
    }

    public void testWaitFollowsPriorityAndEqualPriorityOrder() {
        calculator.recordResolutionTime(60);
        assertEquals(0L, calculator.getEstimatedWaitTime(pending.toList(), "2", null));
        assertEquals(60L, calculator.getEstimatedWaitTime(pending.toList(), "3", null));
        assertEquals(120L, calculator.getEstimatedWaitTime(pending.toList(), "1", null));
        assertEquals(3, pending.size());
        assertEquals("2", pending.peek().getId());
    }

    public void testActiveTicketRemainingTime() {
        calculator.recordResolutionTime(60);
        assertEquals(40L, calculator.getEstimatedWaitTime(pending.toList(), "2", 20L));
        assertEquals(100L, calculator.getEstimatedWaitTime(pending.toList(), "3", 20L));
    }

    public void testOverdueActiveTicketNeverProducesNegativeWait() {
        calculator.recordResolutionTime(60);
        assertEquals(0L, calculator.getEstimatedWaitTime(pending.toList(), "2", 90L));
        assertEquals(60L, calculator.getEstimatedWaitTime(pending.toList(), "3", 90L));
    }

    public void testWaitRecalculatesAfterInterleavedOperations() {
        calculator.recordResolutionTime(30);
        assertEquals("2", pending.dequeue().getId());
        pending.enqueue(new Ticket("4", "Network", "New critical", "critical"));
        assertEquals(0L, calculator.getEstimatedWaitTime(pending.toList(), "3", null));
        assertEquals(30L, calculator.getEstimatedWaitTime(pending.toList(), "4", null));
        assertEquals(60L, calculator.getEstimatedWaitTime(pending.toList(), "1", null));
        assertEquals("3", pending.dequeue().getId());
        pending.enqueue(new Ticket("5", "Other", "Medium issue", "medium"));
        assertEquals(30L, calculator.getEstimatedWaitTime(pending.toList(), "5", null));
        assertEquals(60L, calculator.getEstimatedWaitTime(pending.toList(), "1", null));
    }

    public void testIdNormalization() {
        calculator.recordResolutionTime(60);
        assertEquals(0L, calculator.getEstimatedWaitTime(pending.toList(), " 002 ", null));
    }

    public void testInvalidIdsRejectedEvenWithoutData() {
        for (String id : new String[] {null, "", " ", "abc", "0", "-1", "99999999999", "99"}) {
            expectInvalid(() -> calculator.getEstimatedWaitTime(pending.toList(), id, null));
        }
        assertEquals(-1L, calculator.getAverageResolutionTime());
        assertEquals(3, pending.size());
    }

    public void testCompletedAndCurrentIdsAreNotPending() {
        calculator.recordResolutionTime(30);
        Ticket current = pending.dequeue();
        expectInvalid(() -> calculator.getEstimatedWaitTime(pending.toList(), current.getId(), 0L));
        current.updateStatus("Closed");
        expectInvalid(() -> calculator.getEstimatedWaitTime(pending.toList(), current.getId(), null));
        assertEquals(2, pending.size());
    }

    public void testInvalidElapsedTimeAndNullList() {
        calculator.recordResolutionTime(30);
        expectInvalid(() -> calculator.getEstimatedWaitTime(pending.toList(), "2", -1L));
        expectInvalid(() -> calculator.getEstimatedWaitTime(null, "2", null));
        assertEquals(30L, calculator.getAverageResolutionTime());
    }

    public void testEmptyOrMalformedPendingList() {
        expectInvalid(() -> calculator.getEstimatedWaitTime(Collections.emptyList(), "1", null));
        expectInvalid(() -> calculator.getEstimatedWaitTime(Arrays.asList((Ticket)null), "1", null));
        Ticket closed = new Ticket("4", "Other", "Closed issue", "low");
        closed.updateStatus("Closed");
        expectInvalid(() -> calculator.getEstimatedWaitTime(Arrays.asList(closed), "4", null));
        Ticket duplicate = pending.peek();
        expectInvalid(() -> calculator.getEstimatedWaitTime(Arrays.asList(duplicate, duplicate), "2", null));
    }

    public void testSeparateSessionHasNoData() {
        calculator.recordResolutionTime(30);
        assertEquals(-1L, new ResolutionTimeCalculator().getAverageResolutionTime());
    }
}
