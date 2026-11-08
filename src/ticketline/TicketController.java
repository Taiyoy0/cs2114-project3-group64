package ticketline;

import java.util.List;
import java.util.Objects;

// -------------------------------------------------------------------------
/**
 * Intermediate between ConsoleView and HelpDeskModel
 * 
 * @author taiyol
 * @version Nov 8, 2026
 */
public class TicketController {
    // ~ Fields ................................................................

    private static final String SAVE_FILE = "tickets.txt";
    private static final String NO_PENDING_MESSAGE = "No pending tickets.";

    private final HelpDeskModel model;
    private final ConsoleView view;
    private boolean running;

    // ~ Constructors ..........................................................

    /**
     * Creates instance of TicketController to run the application
     * 
     * @param model
     *            The instance of HelpDeskModel
     * @param view
     *            The instance of ConsoleView
     */
    public TicketController(HelpDeskModel model, ConsoleView view) {
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.view = Objects.requireNonNull(view, "view must not be null");
    }

    // ~Public Methods ........................................................


    /**
     * Entry point for application
     */
    public static void main(String[] args) {
        HelpDeskModel model = new HelpDeskModel();
        ConsoleView view = new ConsoleView();
        new TicketController(model, view).start();
    }


    /** Loads any saved tickets, then runs the main menu loop. */
    public void start() {
        try {
            model.loadTickets(SAVE_FILE);
        }
        catch (Exception e) {
            view.displayError("Could not load saved tickets: " + e.getMessage()
                + ". Starting with an empty queue.");
        }
        run();
    }


    /** Main menu loop. Runs until handleExit() clears the running flag. */
    public void run() {
        running = true;
        while (running) {
            view.displayMenu();
            handleMenuChoice(view.promptMenuChoice());
        }
    }


    /**
     * Routes a raw menu selection. Invalid input changes no model data.
     * 
     * @param choice
     *            menu choice given by the view
     */
    public void handleMenuChoice(String choice) {
        if (choice == null) {
            handleExit();
            return;
        }
        switch (choice.trim()) {
            case "1":
                handleSubmittedTicket();
                break;
            case "2":
                handleViewPending();
                break;
            case "3":
                handleViewNext();
                break;
            case "4":
                handleProcessNext();
                break;
            case "5":
                handleExit();
                break;
            default:
                view.displayError(
                    "Invalid selection. Please enter a number from 1 to 5.");
                break;
        }
    }


    /**
     * Collects ticket info from the view and submits it to the model.
     */
    public void handleSubmittedTicket() {
        boolean finished = false;
        while (!finished) {
            String id = readPositiveId();
            String category = view.promptCategory();
            String description = view.promptDescription();
            // TODO (if severity joins the MVP): String severity =
            // view.promptSeverity();
            // and pass it to model.submitTicket(...) below.

            try {
                if (model.submitTicket(id, category, description)) {
                    view.displayMessage("Ticket " + id + " submitted.");
                    finished = true;
                }
                else {
                    view.displayError(
                        "Ticket could not be submitted. Check that the ID "
                            + "is unused and the category and description are valid.");
                }
            }
            catch (IllegalArgumentException e) {
                view.displayError(e.getMessage());
            }

            if (!finished && !askYesNo("Try again? (y/n)")) {
                view.displayMessage("Ticket submission cancelled.");
                finished = true;
            }
        }
    }


    /** Displays every pending ticket in queue order. */
    public void handleViewPending() {
        List<Ticket> pending = model.getPendingTickets();
        if (pending.isEmpty()) {
            view.displayMessage(NO_PENDING_MESSAGE);
            return;
        }
        view.displayMessage("Pending tickets (" + pending.size() + "):");
        for (Ticket ticket : pending) {
            showTicket(ticket);
        }
    }


    /** Displays the next ticket without removing it. */
    public void handleViewNext() {
        if (!model.hasPendingTickets()) {
            view.displayMessage(NO_PENDING_MESSAGE);
            return;
        }
        try {
            view.displayMessage("Next ticket:");
            showTicket(model.getNextTicket());
        }
        catch (IllegalStateException e) {
            view.displayMessage(NO_PENDING_MESSAGE);
        }
    }


    /** Processes the next ticket and displays the result. */
    public void handleProcessNext() {
        if (!model.hasPendingTickets()) {
            view.displayMessage(NO_PENDING_MESSAGE);
            return;
        }
        try {
            Ticket processed = model.processNextTicket();
            view.displayMessage("Processed ticket:");
            showTicket(processed);
        }
        catch (IllegalStateException e) {
            view.displayMessage(NO_PENDING_MESSAGE);
        }
    }


    /**
     * Saves tickets and ends the application. Offers to stay if saving fails.
     */
    public void handleExit() {
        boolean saved = false;
        while (!saved) {
            try {
                model.saveTickets(SAVE_FILE);
                saved = true;
            }
            catch (Exception e) {
                view.displayError("Could not save tickets: " + e.getMessage());
                if (askYesNo("Exit without saving? (y/n)")) {
                    break;
                }
                if (!askYesNo("Try saving again? (y/n)")) {
                    return;
                }
            }
        }
        view.displayMessage("Goodbye.");
        running = false;
    }

    // ---------------------------------------------------------------- helpers


    /**
     * Prompts until the user enters a positive whole number; returns it
     * normalized.
     */
    private String readPositiveId() {
        while (true) {
            String raw = view.promptTicketId();
            try {
                if (raw == null) {
                    throw new NumberFormatException("null input");
                }
                int value = Integer.parseInt(raw.trim());
                if (value > 0) {
                    return String.valueOf(value);
                }
            }
            catch (NumberFormatException e) {
                // fall through to the error message below
            }
            view.displayError("Ticket ID must be a positive whole number.");
        }
    }


    private boolean askYesNo(String question) {
        String answer = view.promptYesNo(question);
        return answer != null && answer.trim().toLowerCase().startsWith("y");
    }


    /**
     * Converts a Ticket to plain Strings so the view never sees a model object.
     */
    private void showTicket(Ticket ticket) {
        view.displayTicket(String.valueOf(ticket.getID()), String.valueOf(ticket
            .getCategory()), String.valueOf(ticket.getDescription()));
    }

}
