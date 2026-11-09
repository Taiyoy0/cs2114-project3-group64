import junit.framework.TestCase;

/**
 * Tests the ConsoleView class.
 *
 * @author Samuel Abiy
 * @version 11-8-26
 */
public class ConsoleViewTest
    extends TestCase
{
    /**
     * The view being tested.
     */
    private ConsoleView view;

    /**
     * Runs before every test case.
     */
    @Override
    public void setUp()
    {
        view = new ConsoleView(null);
    }


    /**
     * Tests displaying the main menu.
     */
    public void testDisplayMenu()
    {
        String result = view.displayMenu();

        assertTrue(result.contains("TICKETLINE"));
        assertTrue(result.contains("1. Submit a Ticket"));
        assertTrue(result.contains("2. View Pending Tickets"));
        assertTrue(result.contains("3. View Next Ticket"));
        assertTrue(result.contains("4. Process Next Ticket"));
        assertTrue(result.contains("5. Exit"));
    }


    /**
     * Tests displaying a submitted ticket.
     */
    public void testDisplayTicketSubmitted()
    {
        String result = view.displayTicketSubmitted("101");

        assertEquals("Ticket #101 was successfully submitted!", result);
    }


    /**
     * Tests displaying multiple pending tickets.
     */
    public void testDisplayPendingTickets()
    {
        String[][] tickets =
            { { "101", "Software", "Computer issue", "High", "Open" },
                { "102", "Hardware", "Broken keyboard", "Low", "Open" } };

        String result = view.displayPendingTickets(tickets);

        assertTrue(result.contains("101"));
        assertTrue(result.contains("102"));
        assertTrue(result.contains("Software"));
        assertTrue(result.contains("Hardware"));
        assertTrue(result.contains("Computer issue"));
        assertTrue(result.contains("Broken keyboard"));
    }


    /**
     * Tests displaying pending tickets when the list is empty.
     */
    public void testDisplayPendingTicketsEmpty()
    {
        String[][] tickets = {};

        String result = view.displayPendingTickets(tickets);

        assertTrue(result.contains("There are no pending tickets."));
    }


    /**
     * Tests displaying the next pending ticket.
     */
    public void testDisplayNextTicket()
    {
        String result = view.displayNextTicket(
            "101",
            "Software",
            "Computer issue",
            "High",
            "Open");

        assertTrue(result.contains("101"));
        assertTrue(result.contains("Software"));
        assertTrue(result.contains("Computer issue"));
        assertTrue(result.contains("High"));
        assertTrue(result.contains("Open"));
    }


    /**
     * Tests displaying a processed ticket.
     */
    public void testDisplayTicketProcessed()
    {
        String result = view.displayTicketProcessed("101");

        assertEquals("Ticket #101 has been successfully processed!", result);
    }


    /**
     * Tests displaying an error message.
     */
    public void testDisplayError()
    {
        String result = view.displayError("Invalid ticket ID.");

        assertEquals("ERROR: Invalid ticket ID.", result);
    }


    /**
     * Tests displaying a general message.
     */
    public void testDisplayMessage()
    {
        String result = view.displayMessage("Welcome to TicketLine.");

        assertEquals("Welcome to TicketLine.", result);
    }


    /**
     * Tests displaying the no pending tickets message.
     */
    public void testDisplayNoPendingTickets()
    {
        String result = view.displayNoPendingTickets();

        assertEquals("No pending tickets.", result);
    }


    /**
     * Tests displaying the exit message.
     */
    public void testDisplayExitMessage()
    {
        String result = view.displayExitMessage();

        assertTrue(result.contains("Thank you for using TicketLine!"));
        assertTrue(result.contains("Goodbye."));
    }
}
