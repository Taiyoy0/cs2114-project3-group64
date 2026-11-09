package ticketline;

import java.io.InputStream;
import java.io.PrintStream;

import java.util.Scanner;

// -------------------------------------------------------------------------

/**
 * Creating the view, displaying all user interaction. Receives input from user
 * to send to TicketController or receives input from TicketController to output
 * to the user.
 *
 * @author Samuel
 * @version Nov 8, 2026
 */

public class ConsoleView
{
    // ~ Fields ................................................................

    private Scanner input;
    private PrintStream output;
    private boolean inputExhausted;

    // ~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Create a new ConsoleView object.
     *
     */

    public ConsoleView() {
        this(System.in, System.out);
    }

    /** Supplies streams for repeatable tests without changing global input/output. */
    public ConsoleView(InputStream source, PrintStream destination) {
        input = new Scanner(source);
        output = destination;
    }

    private String readInput() {
        if (!input.hasNextLine()) {
            inputExhausted = true;
            return null;
        }
        return input.nextLine();
    }

    /** Reports end of input without attempting another blocking read. */
    public boolean isInputExhausted() { return inputExhausted; }

    /** Collects raw severity; validation belongs in the model. */
    public String getSeverity() {
        output.print("Severity (low, medium, high, critical): ");
        return readInput();
    }

    /** Displays a controller question and returns raw input. */
    public String promptYesNo(String question) {
        output.print(question + " ");
        return readInput();
    }


    /** Displays a completed ticket without labeling it as the next ticket. */
    public String displayTicketDetails(String id, String category,
        String description, String priority, String status) {
        String result = "\n========== TICKET DETAILS ==========\n"
            + "Ticket ID: " + id + "\nCategory: " + category
            + "\nDescription: " + description + "\nPriority: " + priority
            + "\nStatus: " + status + "\n=================================\n";
        output.print(result);
        return result;
    }

    // ~Public Methods ........................................................


    // ----------------------------------------------------------
    /**
     * Displays the menu for user to to pick
     *
     * @return The displayed menu
     */

    public String displayMenu()
    {
        String result = "\n================================\n"
            + "          TICKETLINE\n" + "================================\n"
            + "1. Submit a Ticket\n" + "2. View Pending Tickets\n"
            + "3. View Next Ticket\n" + "4. Process Next Ticket\n" + "5. Exit\n"
            + "================================\n" + "Enter your choice: ";

        output.print(result);
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Reads for useer to input their user choice
     *
     * @return The menu selection
     */

    public String getMenuChoice()
    {
        return readInput();
    }


    // ----------------------------------------------------------
    /**
     * Asks the user for the ticket ID
     *
     * @return The entered ticket ID
     */

    public String getTicketID()
    {
        output.print("Enter Ticket ID: ");
        return readInput();
    }


    // ----------------------------------------------------------
    /**
     * Asks the user for an issue category
     *
     * @return The entered category
     */

    public String getCategory()
    {
        output.println("\nAvailable Categories:");
        output.println("Hardware");
        output.println("Software");
        output.println("Network");
        output.println("Account");
        output.println("Other");
        output.print("Enter category: ");

        return readInput();
    }


    // ----------------------------------------------------------
    /**
     * Asks the user for an issue description
     *
     * @return The entered description
     */

    public String getDescription()
    {
        output.print("Describe your issue: ");

        return readInput();
    }


    // ----------------------------------------------------------
    /**
     * Displays the ticket that becomes submitted
     * 
     * @param id
     *            the submitted ID
     * @return message The ticket was submitted
     */

    public String displayTicketSubmitted(String id)
    {
        String message = "Ticket #" + id + " was successfully submitted!";

        output.println("\n" + message);

        return message;
    }


    // ----------------------------------------------------------
    /**
     * Displays all pending tickets.
     *
     * @param tickets
     *            The ticket information in a row of Strings
     * @return The formatted pending tickets
     */

    public String displayPendingTickets(String[][] tickets)
    {
        StringBuilder result = new StringBuilder();

        result.append("\n============== Pending Tickets ==============\n");

        if (tickets.length == 0)
        {
            result.append("There are no pending tickets.\n");
        }
        else
        {
            for (String[] ticket : tickets)
            {
                result.append("----------------------------\n");
                result.append("Ticket ID: ").append(ticket[0]).append("\n");
                result.append("Category: ").append(ticket[1]).append("\n");
                result.append("Description: ").append(ticket[2]).append("\n");
                result.append("Priority: ").append(ticket[3]).append("\n");
                result.append("Status: ").append(ticket[4]).append("\n");
            }
        }

        result.append("=============================\n");

        output.print(result.toString());

        return result.toString();
    }


    // ----------------------------------------------------------
    /**
     * Displays the information of the next ticket
     *
     * @param id
     *            Ticket ID
     * @param category
     *            Ticket Category
     * @param description
     *            Ticket Description
     * @param priority
     *            Tickets Priority
     * @param status
     *            Ticket Status
     * @return The formatted ticket information
     */

    public String displayNextTicket(
        String id,
        String category,
        String description,
        String priority,
        String status)
    {
        String result = "\n========== NEXT TICKET ==========\n" + "Ticket ID: "
            + id + "\n" + "Category: " + category + "\n" + "Description: "
            + description + "\n" + "Priority: " + priority + "\n" + "Status: "
            + status + "\n" + "=================================\n";

        output.print(result);

        return result;
    }


    // ----------------------------------------------------------
    /**
     * Displays a message that the Ticket has been properly processed
     *
     * @param id
     *            Ticket ID
     * @return The processing confirmation message
     */

    public String displayTicketProcessed(String id)
    {
        String result = "Ticket #" + id + " has been successfully processed!";

        output.println(result);

        return result;
    }


    // ----------------------------------------------------------
    /**
     * Displays an error message
     *
     * @param message
     * @return Displays the error message
     */

    public String displayError(String message)
    {
        String result = "ERROR: " + message;

        output.println("\n" + result);

        return result;
    }


    // ----------------------------------------------------------
    /**
     * Displays a given message
     *
     * @param message
     * @return The displayed message
     */

    public String displayMessage(String message)
    {
        output.println(message);

        return message;
    }


    // ----------------------------------------------------------
    /**
     * Displays when no tickets are pending
     *
     * @return The no pending tickets message
     */

    public String displayNoPendingTickets()
    {
        String result = "No pending tickets.";

        output.println(result);

        return result;
    }


    // ----------------------------------------------------------
    /**
     * Displays the final exit message
     *
     * @return The exit message
     */

    public String displayExitMessage()
    {
        String result = "\nThank you for using TicketLine!\n" + "Goodbye.";

        output.println(result);

        return result;
    }


    // ----------------------------------------------------------
    /**
     * Closes the scanner
     */

    public void close()
    {
        input.close();
    }

}
