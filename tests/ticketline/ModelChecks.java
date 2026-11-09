package ticketline;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Model-only regression checks; no view or controller required. */
public class ModelChecks {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) { throw new AssertionError(message); }
    }
    private static void invalid(Runnable action) {
        try { action.run(); throw new AssertionError("Expected validation failure"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    public static void main(String[] args) throws Exception {
        HelpDeskModel model = new HelpDeskModel();
        model.submitTicket(new String("1"), "Software", "a", "low");
        invalid(() -> model.submitTicket(new String("1"), "Software", "duplicate", "high"));
        invalid(() -> model.submitTicket("001", "Software", "duplicate", "high"));
        model.submitTicket("2", "Hardware", "b", "critical");
        model.submitTicket("3", "Network", "c", "critical");
        check(model.peekNextTicket().getId().equals("2"), "Highest priority first");
        check(model.getPendingTickets().size() == 3, "Peek preserves queue");
        check(model.processNextTicket().getId().equals("2"), "Process highest");
        check(model.peekNextTicket().getId().equals("3"), "Equal priority FIFO");
        check(model.getCompletedTickets().get(0).getStatus().equals("Closed"), "Close status");
        invalid(() -> model.submitTicket(new String("2"), "Software", "duplicate completed", "low"));
        List<Ticket> snapshot = model.getPendingTickets(); snapshot.clear();
        check(model.getPendingTickets().size() == 2, "Snapshot membership isolated");
        invalid(() -> new Ticket("0", "Other", "a", "low"));
        invalid(() -> new Ticket("abc", "Other", "a", "low"));
        invalid(() -> new Ticket("4", "Other", " ", "low"));
        invalid(() -> new Ticket("4", "Other", new String(""), "low"));
        invalid(() -> new Ticket("4", "invalid", "a", "low"));
        invalid(() -> new Ticket("4", "Other", "a", "urgent"));
        Ticket a = new Ticket("4", "Other", "original", " HIGH ");
        invalid(() -> a.updateDescription(" "));
        invalid(() -> a.updateStatus("invalid"));
        check(a.getDescription().equals("original") && a.getStatus().equals("Open"), "Failed updates preserve state");
        Queue<Ticket> queue = new Queue<>();
        invalid(() -> queue.enqueue(null));
        try { queue.dequeue(); throw new AssertionError("Expected empty exception"); }
        catch (IllegalStateException expected) { checks++; }
        try { queue.peek(); throw new AssertionError("Expected empty exception"); }
        catch (IllegalStateException expected) { checks++; }
        queue.enqueue(a); check(queue.dequeue() == a && queue.isEmpty() && queue.size() == 0, "Queue empty reset");
        queue.enqueue(a); check(queue.peek() == a, "Queue reuse");

        Path dir = Files.createTempDirectory("ticketline-checks");
        Path file = dir.resolve("tickets.txt");
        model.submitTicket("4", "Account", "comma, quote \" and\nnewline", "medium");
        model.savePendingTickets(file.toString());
        HelpDeskModel restored = new HelpDeskModel(); restored.loadPendingTickets(file.toString());
        check(restored.getPendingTickets().size() == 3, "Restart count");
        check(restored.getPendingTickets().get(1).getDescription().equals("comma, quote \" and\nnewline"), "CSV special characters");
        check(restored.peekNextTicket().getId().equals("3"), "Restart priority");
        restored.loadPendingTickets(file.toString());
        check(restored.getPendingTickets().size() == 3, "Reload replaces pending safely");
        Files.write(file, Arrays.asList("5, Other, legacy, high, Open"), StandardCharsets.UTF_8);
        restored.loadPendingTickets(file.toString());
        check(restored.peekNextTicket().getId().equals("5"), "Read old save format");
        Files.write(file, Arrays.asList("6, Other, valid, low, Open", "broken"), StandardCharsets.UTF_8);
        try { restored.loadPendingTickets(file.toString()); throw new AssertionError("Malformed accepted"); }
        catch (IOException expected) { checks++; }
        check(restored.peekNextTicket().getId().equals("5"), "Failed load preserves model");
        Files.write(file, Arrays.asList("7, Other, a, low, Open", "007, Other, b, high, Open"), StandardCharsets.UTF_8);
        try { restored.loadPendingTickets(file.toString()); throw new AssertionError("Duplicate load accepted"); }
        catch (IOException expected) { checks++; }
        restored.loadPendingTickets(dir.resolve("missing.txt").toString());
        check(restored.peekNextTicket().getId().equals("5"), "Missing file no-op");
        try { restored.savePendingTickets(dir.toString()); throw new AssertionError("Expected write failure"); }
        catch (IOException expected) { checks++; }

        check(model.hasPendingTickets(), "Pending flag");
        List<Ticket> completed = model.getCompletedTickets(); completed.clear();
        check(model.getCompletedTickets().size() == 1, "Completed snapshot membership isolated");
        invalid(() -> new Ticket(null, "Other", "a", "low"));
        invalid(() -> new Ticket("8", null, "a", "low"));
        invalid(() -> new Ticket("8", "Other", null, "low"));
        invalid(() -> new Ticket("8", "Other", "a", null));
        invalid(() -> a.updateDescription(null));
        invalid(() -> a.updateStatus(null));
        HelpDeskModel empty = new HelpDeskModel();
        try { empty.processNextTicket(); throw new AssertionError("Expected empty exception"); }
        catch (IllegalStateException expected) { checks++; }
        try { empty.peekNextTicket(); throw new AssertionError("Expected empty exception"); }
        catch (IllegalStateException expected) { checks++; }
        check(!empty.hasPendingTickets() && empty.getCompletedTickets().isEmpty(), "Empty actions preserve state");
        Path emptyFile = dir.resolve("empty.txt");
        empty.savePendingTickets(emptyFile.toString());
        restored.loadPendingTickets(emptyFile.toString());
        check(!restored.hasPendingTickets(), "Empty save replaces pending queue");
        Files.deleteIfExists(file); Files.deleteIfExists(emptyFile); Files.delete(dir);
        System.out.println("PASS: " + checks + " model checks");
    }
}
