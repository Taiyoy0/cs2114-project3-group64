package ticketline;

import junit.framework.TestCase;
import java.time.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;

/** Direct lifecycle, calculations, validation and persistence tests; no view. */
public class HelpDeskModelTest extends TestCase {
    /** Test-only adjustable clock: advancing it requires no sleeping. */
    public static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");
        public void advance(long seconds) { now = now.plusSeconds(seconds); }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }
        public Instant instant() { return now; }
    }
    private MutableClock clock;
    private HelpDeskModel model;
    private Path directory;
    private Path file;
    private interface Action { void run() throws Exception; }

    private void rejects(Class<? extends Exception> type, Action action) throws Exception {
        try { action.run(); fail("Expected " + type.getSimpleName()); }
        catch (Exception e) { if (!type.isInstance(e)) { throw e; } }
    }
    @Override
    protected void setUp() throws Exception {
        clock = new MutableClock();
        model = new HelpDeskModel(clock);
        directory = Files.createTempDirectory("ticketline-model-test");
        file = directory.resolve("tickets.txt");
    }
    @Override
    protected void tearDown() throws Exception {
        try (java.util.stream.Stream<Path> files = Files.walk(directory)) {
            for (Path path : (Iterable<Path>)files.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(path);
            }
        }
    }
    private void submit(String id, String severity) {
        model.submitTicket(id, "Other", "Issue " + id, severity);
    }
    private void complete(long seconds) {
        model.startNextTicket(); clock.advance(seconds); model.completeCurrentTicket();
    }
    private void write(String text) throws IOException {
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));
    }
    public void testEmptyState() {
        assertFalse(model.hasPendingTickets());
        assertNull(model.getCurrentTicket());
        assertTrue(model.getPendingTickets().isEmpty());
        assertTrue(model.getCompletedTickets().isEmpty());
        assertEquals(-1L, model.getAverageResolutionTime());
        assertFalse(model.idExists("1"));
    }
    public void testNullClockRejected() throws Exception {
        rejects(IllegalArgumentException.class, () -> new HelpDeskModel(null));
    }
    public void testSubmitAndPeekPreservesQueue() {
        submit("1", "low"); submit("2", "critical");
        assertEquals("2", model.peekNextTicket().getId());
        assertEquals(2, model.getPendingTickets().size());
        assertTrue(model.hasPendingTickets());
        assertTrue(model.idExists("002"));
    }
    public void testSubmitInvalidFieldsDoesNotChangeState() throws Exception {
        String[][] bad = {{null,"Other","ok","low"},{"0","Other","ok","low"},
            {"-1","Other","ok","low"},{"abc","Other","ok","low"},
            {"999999999999","Other","ok","low"},{"1",null,"ok","low"},
            {"1","wrong","ok","low"},{"1","Other",null,"low"},
            {"1","Other","   ","low"},{"1","Other","ok",null},
            {"1","Other","ok","urgent"}};
        for (String[] row : bad) {
            rejects(IllegalArgumentException.class, () -> model.submitTicket(row[0],row[1],row[2],row[3]));
        }
        assertFalse(model.hasPendingTickets());
        assertNull(model.getCurrentTicket());
    }
    public void testDuplicateIdsAcrossAllStates() throws Exception {
        submit("1", "high");
        rejects(IllegalArgumentException.class, () -> submit("001", "low"));
        model.startNextTicket();
        assertTrue(model.idExists(" 001 "));
        rejects(IllegalArgumentException.class, () -> submit("1", "low"));
        model.completeCurrentTicket();
        assertTrue(model.idExists("1"));
        rejects(IllegalArgumentException.class, () -> submit("1", "critical"));
        assertEquals(1, model.getCompletedTickets().size());
    }
    public void testIdExistsRejectsInvalidInput() throws Exception {
        rejects(IllegalArgumentException.class, () -> model.idExists(null));
        rejects(IllegalArgumentException.class, () -> model.idExists("abc"));
    }
    public void testStartAndCompleteWithKnownDuration() {
        submit("1", "high");
        Ticket started = model.startNextTicket();
        assertSame(started, model.getCurrentTicket());
        assertEquals("In Progress", started.getStatus());
        assertFalse(model.hasPendingTickets());
        assertTrue(model.getPendingTickets().isEmpty());
        clock.advance(90);
        assertSame(started, model.completeCurrentTicket());
        assertNull(model.getCurrentTicket());
        assertEquals("Closed", started.getStatus());
        assertEquals(90L, started.getResolutionDuration().getSeconds());
        assertEquals(90L, model.getAverageResolutionTime());
    }
    public void testEmptyOperationsRejected() throws Exception {
        rejects(IllegalStateException.class, () -> model.peekNextTicket());
        rejects(IllegalStateException.class, () -> model.startNextTicket());
        rejects(IllegalStateException.class, () -> model.completeCurrentTicket());
        rejects(IllegalStateException.class, () -> model.processNextTicket());
        assertNull(model.getCurrentTicket());
        assertTrue(model.getCompletedTickets().isEmpty());
    }
    public void testDoubleStartPreservesCurrentAndPending() throws Exception {
        submit("1", "high"); submit("2", "low");
        Ticket active = model.startNextTicket();
        rejects(IllegalStateException.class, () -> model.startNextTicket());
        rejects(IllegalStateException.class, () -> model.processNextTicket());
        assertSame(active, model.getCurrentTicket());
        assertEquals("2", model.peekNextTicket().getId());
    }
    public void testDoubleCompleteDoesNotRecordTwice() throws Exception {
        submit("1", "low"); complete(60);
        rejects(IllegalStateException.class, () -> model.completeCurrentTicket());
        assertEquals(1, model.getCompletedTickets().size());
        assertEquals(60L, model.getAverageResolutionTime());
    }
    public void testAverageExcludesPendingAndActiveAndRoundsDown() {
        submit("1", "critical"); submit("2", "high"); submit("3", "low");
        complete(60); complete(121);
        model.startNextTicket(); clock.advance(500);
        assertEquals(90L, model.getAverageResolutionTime());
    }
    public void testNoAverageUntilCompletionAndZeroValid() {
        submit("1", "high"); submit("2", "low");
        assertEquals(-1L, model.getEstimatedWaitTime("2"));
        model.startNextTicket(); clock.advance(60);
        assertEquals(-1L, model.getAverageResolutionTime());
        model.completeCurrentTicket();
        model.startNextTicket(); model.completeCurrentTicket();
        assertEquals(30L, model.getAverageResolutionTime());
    }
    public void testWaitChangesWithPriorityActiveElapsedAndArrival() {
        submit("1", "critical"); complete(60);
        submit("2", "low"); submit("3", "high"); submit("4", "high");
        assertEquals(0L, model.getEstimatedWaitTime("3"));
        assertEquals(60L, model.getEstimatedWaitTime("4"));
        assertEquals(120L, model.getEstimatedWaitTime("2"));
        model.startNextTicket(); clock.advance(20);
        assertEquals(40L, model.getEstimatedWaitTime("004"));
        submit("5", "critical");
        assertEquals(100L, model.getEstimatedWaitTime("4"));
        clock.advance(100);
        assertEquals(0L, model.getEstimatedWaitTime("5"));
        assertEquals(60L, model.getEstimatedWaitTime("4"));
        assertEquals("3", model.getCurrentTicket().getId());
    }
    public void testWaitRejectsInvalidMissingCurrentAndCompletedIds() throws Exception {
        submit("1", "high"); complete(30); submit("2", "high"); model.startNextTicket();
        for (String id : new String[] {null,""," ","-1","abc","99","1","2"}) {
            rejects(IllegalArgumentException.class, () -> model.getEstimatedWaitTime(id));
        }
        assertEquals("2", model.getCurrentTicket().getId());
        assertEquals(30L, model.getAverageResolutionTime());
    }
    public void testInterleavedModelOperationsAndSnapshots() {
        submit("1", "low"); submit("2", "high"); complete(10);
        submit("3", "medium"); submit("4", "critical");
        assertEquals("4", model.startNextTicket().getId()); model.completeCurrentTicket();
        submit("5", "medium");
        assertEquals("3", model.peekNextTicket().getId());
        model.getPendingTickets().clear(); model.getCompletedTickets().clear();
        assertEquals(3, model.getPendingTickets().size());
        assertEquals(2, model.getCompletedTickets().size());
        assertEquals("3", model.processNextTicket().getId());
        assertEquals("5", model.peekNextTicket().getId());
    }
    public void testSaveLoadSpecialCharactersAndReload() throws Exception {
        model.submitTicket("1","Other","comma, quote \" newline\nUnicode café", "low");
        submit("2", "critical");
        model.savePendingTickets(file.toString());
        HelpDeskModel restored = new HelpDeskModel(clock); restored.loadPendingTickets(file.toString());
        restored.loadPendingTickets(file.toString());
        assertEquals(2, restored.getPendingTickets().size());
        assertEquals("2", restored.peekNextTicket().getId());
        assertEquals("comma, quote \" newline\nUnicode café",restored.getPendingTickets().get(1).getDescription());
    }
    public void testActiveSaveRestoresAsOpenInEqualPriorityOrder() throws Exception {
        submit("1", "high"); submit("2", "high"); model.startNextTicket();
        clock.advance(100); submit("3", "critical");
        model.savePendingTickets(file.toString());
        assertEquals("In Progress", model.getCurrentTicket().getStatus());
        HelpDeskModel restored = new HelpDeskModel(clock); restored.loadPendingTickets(file.toString());
        assertNull(restored.getCurrentTicket());
        assertEquals("3",restored.processNextTicket().getId());
        assertEquals("1",restored.startNextTicket().getId());
        assertEquals(0L,restored.getCurrentTicket().getElapsedProcessingSeconds());
        clock.advance(20); restored.completeCurrentTicket();
        assertEquals(10L,restored.getAverageResolutionTime());
        assertEquals("2",restored.peekNextTicket().getId());
    }
    public void testStatisticsAndCompletedHistoryResetOnRestart() throws Exception {
        submit("1", "low"); complete(60); submit("2", "low");
        model.savePendingTickets(file.toString());
        HelpDeskModel restored = new HelpDeskModel(clock); restored.loadPendingTickets(file.toString());
        assertEquals(-1L,restored.getAverageResolutionTime());
        assertTrue(restored.getCompletedTickets().isEmpty());
        assertFalse(restored.idExists("1"));
    }
    public void testLegacyActiveRowWrittenLastRestoresFirstAmongPeers() throws Exception {
        write("2, Other, waiting, high, Open\n1, Other, active, high, In progress\n");
        model.loadPendingTickets(file.toString());
        assertEquals("1",model.peekNextTicket().getId());
        assertEquals("Open",model.peekNextTicket().getStatus());
        assertNull(model.getCurrentTicket());
    }
    public void testMalformedFilesLeaveExistingStateUntouched() throws Exception {
        submit("9", "low");
        String[] bad = {"broken", "1, Other, a, high, Open\nbroken",
            "1, Other, a, low, Open\n001, Other, b, low, Open",
            "1, Other, a, low, Closed", "1, bad, a, low, Open",
            "1, Other, a, urgent, Open", "1, Other,  , low, Open",
            "abc, Other, a, low, Open", "\"unterminated",
            "1, Other, a, high, In progress\n2, Other, b, low, In Progress",
            "1, Other, a, low, unknown"};
        for (String text : bad) {
            write(text);
            rejects(IOException.class, () -> model.loadPendingTickets(file.toString()));
            assertEquals("9",model.peekNextTicket().getId());
            assertEquals(1,model.getPendingTickets().size());
        }
    }
    public void testLoadCannotInterruptActiveTicket() throws Exception {
        submit("1", "low"); model.startNextTicket(); write("2, Other, a, low, Open");
        rejects(IOException.class, () -> model.loadPendingTickets(file.toString()));
        assertEquals("1",model.getCurrentTicket().getId());
    }
    public void testLoadRejectsCompletedId() throws Exception {
        submit("1", "low"); complete(60); write("1, Other, a, low, Open");
        rejects(IOException.class, () -> model.loadPendingTickets(file.toString()));
        assertEquals(60L,model.getAverageResolutionTime());
    }
    public void testMissingAndEmptyFiles() throws Exception {
        submit("1", "low"); model.loadPendingTickets(file.toString());
        assertEquals("1",model.peekNextTicket().getId());
        write(""); model.loadPendingTickets(file.toString());
        assertFalse(model.hasPendingTickets());
        model.savePendingTickets(file.toString());
        assertEquals(0L,Files.size(file));
    }
    public void testInvalidPathsAndFailedSavePreserveModel() throws Exception {
        submit("1", "low");
        for (String path : new String[] {null,""," ",directory.toString(),directory.resolve("missing/sub/file").toString()}) {
            rejects(IOException.class, () -> model.savePendingTickets(path));
        }
        for (String path : new String[] {null,""," ",directory.toString()}) {
            rejects(IOException.class, () -> model.loadPendingTickets(path));
        }
        assertEquals("1",model.peekNextTicket().getId());
    }
    public void testBackwardClockDoesNotLoseActiveTicket() throws Exception {
        submit("1", "low"); model.startNextTicket(); clock.advance(-10);
        assertEquals(0L,model.getCurrentTicket().getElapsedProcessingSeconds());
        rejects(IllegalStateException.class, () -> model.completeCurrentTicket());
        assertEquals("In Progress",model.getCurrentTicket().getStatus());
        assertTrue(model.getCompletedTickets().isEmpty());
        clock.advance(20); model.completeCurrentTicket();
        assertEquals(10L,model.getAverageResolutionTime());
    }
}
