package ticketline;

import junit.framework.TestCase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Exercises Samuel's real view with the controller and model. */
public class ConsoleIntegrationTest extends TestCase {
    private Path directory;
    private Path save;
    private HelpDeskModel model;
    private ByteArrayOutputStream output;

    @Override
    protected void setUp() throws Exception {
        directory = Files.createTempDirectory("ticketline-ui");
        save = directory.resolve("tickets.txt");
        model = new HelpDeskModel();
        output = new ByteArrayOutputStream();
    }

    @Override
    protected void tearDown() throws Exception {
        Files.deleteIfExists(save);
        Files.delete(directory);
    }

    private void run(String input, boolean start) {
        ConsoleView view = new ConsoleView(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            new PrintStream(output));
        TicketController controller = new TicketController(model, view, save.toString());
        if (start) { controller.start(); }
        else { controller.run(); }
    }

    private String text() throws Exception { return output.toString("UTF-8"); }

    public void testFullMvpAndRestart() throws Exception {
        run("1\n1\nSoftware\nApp, won't open\nlow\n"
            + "1\n2\nNetwork\nOffice down\ncritical\n2\n3\n4\n6\n5\n", true);
        assertEquals(1, model.getPendingTickets().size());
        assertEquals("2", model.getCompletedTickets().get(0).getId());
        assertEquals("Closed", model.getCompletedTickets().get(0).getStatus());
        assertTrue(text().contains("Pending Tickets"));
        assertTrue(text().contains("Priority: critical"));
        assertTrue(text().contains("successfully processed"));
        assertTrue(text().contains("TICKET DETAILS"));
        model = new HelpDeskModel();
        run("3\n5\n", true);
        assertEquals("App, won't open", model.peekNextTicket().getDescription());
        assertTrue(text().contains("Goodbye."));
    }

    public void testInvalidMenuAndEmptyOperations() throws Exception {
        run("letters\n99\n2\n3\n4\n5\n", true);
        assertTrue(text().contains("Invalid selection"));
        assertTrue(text().contains("No pending tickets."));
        assertFalse(model.hasPendingTickets());
    }

    public void testPositiveIdRetry() throws Exception {
        run("1\nabc\n0\n-2\n001\nOther\na\nhigh\n5\n", true);
        assertEquals("1", model.peekNextTicket().getId());
        assertTrue(text().contains("positive whole number"));
    }

    public void testInvalidCategoryAndSeverityCancel() throws Exception {
        run("1\n1\nUnknown\na\nlow\nn\n"
            + "1\n2\nOther\nb\nurgent\nn\n5\n", true);
        assertFalse(model.hasPendingTickets());
        assertTrue(text().contains("submission cancelled"));
        assertTrue(text().contains("Invalid severity"));
    }

    public void testBlankDescriptionRetry() throws Exception {
        run("1\n1\nOther\n   \nlow\ny\n1\nOther\nvalid\nlow\n5\n", true);
        assertEquals("valid", model.peekNextTicket().getDescription());
        assertTrue(text().contains("Invalid description"));
    }

    public void testDuplicatePendingAndCompletedId() throws Exception {
        run("1\n1\nOther\na\nlow\n"
            + "1\n001\nOther\nb\nhigh\nn\n4\n6\n"
            + "1\n1\nOther\nc\nlow\nn\n5\n", true);
        assertEquals(1, model.getCompletedTickets().size());
        assertFalse(model.hasPendingTickets());
        assertTrue(text().contains("ID already exists"));
    }

    public void testInputEndsDuringSubmission() throws Exception {
        run("1\n", true);
        assertFalse(model.hasPendingTickets());
        assertTrue(text().contains("Goodbye."));
    }

    public void testSaveFailureAndExplicitExit() throws Exception {
        Files.createDirectory(save);
        run("5\ny\n", false);
        assertTrue(text().contains("Could not save tickets"));
        assertTrue(text().contains("Goodbye."));
    }

    public void testSaveFailureAtEndOfInput() throws Exception {
        Files.createDirectory(save);
        run("", false);
        assertTrue(text().contains("exiting without saving"));
    }

    public void testMalformedLoadReportsError() throws Exception {
        Files.write(save, "broken record".getBytes(StandardCharsets.UTF_8));
        run("5\n", true);
        assertTrue(text().contains("Could not load saved tickets"));
        assertFalse(model.hasPendingTickets());
    }
    public void testTimingWorkflowWithControlledClock() throws Exception {
        HelpDeskModelTest.MutableClock clock = new HelpDeskModelTest.MutableClock();
        model = new HelpDeskModel(clock);
        model.submitTicket("1", "Other", "First", "critical");
        model.submitTicket("2", "Other", "Second", "high");
        model.submitTicket("3", "Other", "Third", "low");
        run("7\n4\n", false); // EOF saves active work, but leaves in-memory state testable.
        assertEquals("1", model.getCurrentTicket().getId());
        assertTrue(text().contains("Not enough data."));
        clock.advance(60);
        output.reset();
        run("6\n7\n2\n4\n", false);
        assertEquals(60L, model.getAverageResolutionTime());
        assertEquals("2", model.getCurrentTicket().getId());
        assertTrue(text().contains("Average Resolution Time: 1m 0s"));
        assertTrue(text().contains("Estimated Wait: 1m 0s"));
        clock.advance(20); output.reset();
        run("2\n5\n", false);
        assertTrue(text().contains("CURRENT TICKET"));
        assertTrue(text().contains("Estimated Wait: 0m 40s"));
    }

    public void testActiveTicketSurvivesConsoleExitAndRestart() throws Exception {
        run("1\n1\nOther\nfirst\nhigh\n1\n2\nOther\nsecond\nhigh\n4\n5\n", true);
        assertEquals("1", model.getCurrentTicket().getId());
        model = new HelpDeskModel(); output.reset();
        run("2\n4\n6\n5\n", true);
        assertNull(model.getCurrentTicket());
        assertEquals("1", model.getCompletedTickets().get(0).getId());
        assertEquals("2", model.peekNextTicket().getId());
        assertTrue(text().contains("successfully processed"));
    }

    public void testDoubleStartAndCompleteWithoutActiveSurvive() throws Exception {
        run("6\n1\n1\nOther\nfirst\nhigh\n1\n2\nOther\nsecond\nlow\n4\n4\n6\n6\n5\n", true);
        assertEquals(1, model.getCompletedTickets().size());
        assertEquals("2", model.peekNextTicket().getId());
        assertTrue(text().contains("Already processing"));
        assertTrue(text().contains("No ticket currently being processed"));
    }

    public void testDuplicateCurrentIdAndSingleActiveStartSurvive() throws Exception {
        run("1\n1\nOther\noriginal\nhigh\n4\n4\n1\n001\nOther\noverwrite\nlow\nn\n2\n7\n5\n", true);
        assertEquals("original", model.getCurrentTicket().getDescription());
        assertTrue(model.getPendingTickets().isEmpty());
        assertTrue(text().contains("ID already exists"));
        assertTrue(text().contains("Already processing"));
        assertTrue(text().contains("CURRENT TICKET"));
    }

    public void testCompletedIdCannotOverwriteOriginalDescription() throws Exception {
        run("1\n1\nOther\noriginal\nhigh\n4\n6\n1\n001\nOther\nreplacement\ncritical\nn\n5\n", true);
        assertEquals("original", model.getCompletedTickets().get(0).getDescription());
        assertFalse(model.hasPendingTickets());
        assertTrue(text().contains("ID already exists"));
    }
}
