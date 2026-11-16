package ticketline;

import junit.framework.TestCase;

/** Direct ticket validation and clock-controlled lifecycle tests. */
public class TicketTest extends TestCase {
    private interface Action { void run(); }
    private void invalid(Action action) {
        try { action.run(); fail("Expected IllegalArgumentException"); }
        catch (IllegalArgumentException expected) { }
    }
    private void illegalState(Action action) {
        try { action.run(); fail("Expected IllegalStateException"); }
        catch (IllegalStateException expected) { }
    }
    private Ticket ticket() { return new Ticket("1","Other","Example","high"); }
    public void testFieldsAreTrimmedAndIdNormalized() {
        Ticket t = new Ticket(" 001 "," Software "," issue "," HIGH ");
        assertEquals("1",t.getId()); assertEquals("Software",t.getCategory());
        assertEquals("issue",t.getDescription()); assertEquals("HIGH",t.getSeverity());
        assertEquals("Open",t.getStatus());
    }
    public void testInvalidIds() {
        for (String id : new String[] {null,""," ","0","-1","abc","1.5","99999999999"}) {
            invalid(() -> new Ticket(id,"Other","ok","low"));
        }
    }
    public void testInvalidCategoryDescriptionSeverityAndClock() {
        for (String value : new String[] {null,""," "}) {
            invalid(() -> new Ticket("1",value,"ok","low"));
            invalid(() -> new Ticket("1","Other",value,"low"));
            invalid(() -> new Ticket("1","Other","ok",value));
        }
        invalid(() -> new Ticket("1","bad","ok","low"));
        invalid(() -> new Ticket("1","Other","ok","urgent"));
        invalid(() -> new Ticket("1","Other","ok","low",null));
    }
    public void testDescriptionUpdateAndInvalidUpdate() {
        Ticket t = ticket(); t.updateDescription(" New issue ");
        assertEquals("New issue",t.getDescription());
        for (String value : new String[] {null,""," "}) { invalid(() -> t.updateDescription(value)); }
        assertEquals("New issue",t.getDescription());
    }
    public void testStatusValidation() {
        Ticket t = ticket(); t.updateStatus("In Progress");
        assertEquals("In Progress",t.getStatus());
        for (String value : new String[] {null,""," ","invalid"}) { invalid(() -> t.updateStatus(value)); }
        assertEquals("In Progress",t.getStatus());
    }
    public void testComparePriorityAndEqualPriority() {
        Ticket t = ticket();
        assertTrue(t.compareTo(new Ticket("2","Other","ok","low")) < 0);
        assertTrue(t.compareTo(new Ticket("2","Other","ok","critical")) > 0);
        assertEquals(0,t.compareTo(new Ticket("2","Other","ok","high")));
        try { t.compareTo(null); fail("Null comparison must be rejected."); }
        catch (NullPointerException expected) { }
    }
    public void testStartCompleteAndStableDuration() {
        HelpDeskModelTest.MutableClock clock = new HelpDeskModelTest.MutableClock();
        Ticket t = new Ticket("1","Other","ok","high",clock);
        t.startProcessing(); assertEquals("In Progress",t.getStatus());
        clock.advance(42); assertEquals(42L,t.getElapsedProcessingSeconds());
        t.completeProcessing(); assertEquals("Closed",t.getStatus());
        assertEquals(42L,t.getResolutionDuration().getSeconds());
        clock.advance(100); assertEquals(42L,t.getElapsedProcessingSeconds());
    }
    public void testLifecycleBadStatesPreserveTicket() {
        Ticket t = ticket();
        illegalState(() -> t.completeProcessing());
        illegalState(() -> t.getResolutionDuration());
        illegalState(() -> t.getElapsedProcessingSeconds());
        assertEquals("Open",t.getStatus());
        t.startProcessing();
        illegalState(() -> t.startProcessing());
        illegalState(() -> t.getResolutionDuration());
        t.completeProcessing();
        illegalState(() -> t.completeProcessing());
        illegalState(() -> t.startProcessing());
        assertEquals("Closed",t.getStatus());
    }
}
