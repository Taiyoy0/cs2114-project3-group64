package ticketline;

import java.nio.file.Files;
import java.nio.file.Path;

/** Tiny model driver, not a replacement for Samuel's ConsoleView. */
public class ModelDemo {
    public static void main(String[] args) throws Exception {
        HelpDeskModel model = new HelpDeskModel();
        model.submitTicket("1", "Software", "Application will not open", "low");
        model.submitTicket("2", "Network", "Office network is down", "critical");
        model.submitTicket("3", "Hardware", "Server is down", "critical");
        System.out.println("Pending in priority order:");
        for (Ticket ticket : model.getPendingTickets()) {
            System.out.println(ticket.getId() + ": " + ticket.getDescription()
                + " (" + ticket.getSeverity() + ")");
        }
        Ticket processed = model.processNextTicket();
        System.out.println("Processed: " + processed.getId() + " - " + processed.getStatus());
        Path file = Files.createTempFile("ticketline-demo", ".csv");
        try {
            model.savePendingTickets(file.toString());
            HelpDeskModel restarted = new HelpDeskModel();
            restarted.loadPendingTickets(file.toString());
            System.out.println("Restored pending: " + restarted.getPendingTickets().size());
            System.out.println("Next after restart: " + restarted.peekNextTicket().getId());
        }
        finally { Files.deleteIfExists(file); }
    }
}
