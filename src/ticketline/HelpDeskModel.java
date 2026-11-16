package ticketline;

import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

// -------------------------------------------------------------------------
/**
 *  A class to modify and manage the open and completed tickets.
 * 
 *  @author vivaandutt
 *  @version Nov 9, 2026
 */
public class HelpDeskModel {
    
    private Queue<Ticket> openTickets;
    private List<Ticket> completedTickets;
    private Ticket currentTicket;
    private final Clock clock;
    private final ResolutionTimeCalculator timing = new ResolutionTimeCalculator();
    
    // ----------------------------------------------------------
    /**
     * Create a new HelpDeskModel object.
     */
    public HelpDeskModel() {
        this(Clock.systemUTC());
    }

    /** All tickets use the same injectable clock; model tests need no UI. */
    public HelpDeskModel(Clock clock) {
        if (clock == null) { throw new IllegalArgumentException("Clock cannot be null."); }
        this.clock = clock;
        openTickets = new Queue<>();
        completedTickets = new ArrayList<>();
    }
    
    // ----------------------------------------------------------
    /**
     * Adds a new ticket to the queue of open tickets.
     * @param id
     * @param category
     * @param description
     * @param severity
     * @return whether or not the new ticket was submitted
     * @throws IllegalArgumentException if given a ticket with a pre-existing id
     */
    public boolean submitTicket(String id, String category, String description, 
        String severity) {
        Ticket newTicket = new Ticket(id, category, description, severity, clock);
        
        if (idExists(newTicket.getId())) {
            throw new IllegalArgumentException("ID already exists.");
        }
        
        openTickets.enqueue(newTicket);
        return true;
    }
    
    // ----------------------------------------------------------
    /**
     * Checks through the open and completed tickets to see if any old ticket id
     * matches the given ticket id
     * @param i the given ticket id
     * @return whether an old ticket has been found with the same given id
     */
    public boolean idExists(String i) {
        i = Ticket.normalizeId(i);
        
        for (Ticket t : openTickets.toList()) {
            if (t.getId().equals(i)) {
                return true;
            }
        }
        
        for (Ticket t : completedTickets) {
            if (t.getId().equals(i)) {
                return true;
            }
        }
        
        if (currentTicket != null && currentTicket.getId().equals(i)) {
            return true;
        }
        
        return false;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the oldest open ticket.
     * @return the oldest open ticket
     */
    public Ticket peekNextTicket() {
        return openTickets.peek();
    }
    
    // ----------------------------------------------------------
    /**
     * Starts processing the next ticket in the queue
     * @throws IllegalStateException if a ticket is already being processed or
     * if there are no tickets to process
     */
    public Ticket startNextTicket() {
        
        if (currentTicket != null) {
            throw new IllegalStateException("Already processing a ticket.");
        }
        if (openTickets.isEmpty()) {
            throw new IllegalStateException("No tickets to process.");
        }
        
        Ticket next = openTickets.peek();
        next.startProcessing();
        currentTicket = openTickets.dequeue();
        return currentTicket;
    }
    
    // ----------------------------------------------------------
    /**
     * Finishes processing the current ticket being processed.
     * @throws IllegalStateException if no ticket is currently being processed
     */
    public Ticket completeCurrentTicket() {
        if (currentTicket == null) {
            throw new IllegalStateException
            ("No ticket currently being processed");
        }
        
        currentTicket.completeProcessing();
        Ticket completed = currentTicket;
        timing.recordResolutionTime(completed.getResolutionDuration().getSeconds());
        completedTickets.add(completed);
        currentTicket = null;
        return completed;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns whether there are queued tickets waiting to start; excludes the active ticket.
     * @return true if openTicket is not empty
     */
    public boolean hasPendingTickets() {
        return !openTickets.isEmpty();
    }
    
    // ----------------------------------------------------------
    /**
     * Returns a list of queued tickets; the active ticket is available separately.
     * @return the entries of openTickets in a list
     */
    public List<Ticket> getPendingTickets() {
        return new ArrayList<>(openTickets.toList());
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the completed tickets as a List.
     * @return the entries of completedTickets
     */
    public List<Ticket> getCompletedTickets() {
        return new ArrayList<>(completedTickets);
    }
    
    // ----------------------------------------------------------
    /**
     * Saves all pending tickets to a local file.
     * @param filepath the location of the file
     * @throws IOException if the filepath doesn't work
     */
    public void savePendingTickets(String filepath) throws IOException {
        Path target = checkedPath(filepath).toAbsolutePath();
        if (Files.isDirectory(target)) { throw new IOException("Save path is a directory."); }
        Path temp = Files.createTempFile(target.getParent(), "ticketline-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                // Active work was removed first, so write it before equal-priority peers.
                if (currentTicket != null) { writeTicket(writer, currentTicket); }
                for (Ticket ticket : openTickets.toList()) { writeTicket(writer, ticket); }
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally { Files.deleteIfExists(temp); }
    }

    /** Persist unfinished work as Open; interrupted timing samples are discarded. */
    private static void writeTicket(BufferedWriter writer, Ticket ticket) throws IOException {
        writer.write(csv(ticket.getId()) + "," + csv(ticket.getCategory()) + ","
            + csv(ticket.getDescription()) + "," + csv(ticket.getSeverity()) + "," + csv("Open"));
        writer.newLine();
    }

    private static Path checkedPath(String filepath) throws IOException {
        if (filepath == null || filepath.trim().isEmpty()) {
            throw new IOException("File path must not be blank.");
        }
        try { return Paths.get(filepath); }
        catch (InvalidPathException e) { throw new IOException("Invalid file path.", e); }
    }

    /** Validates an entire file before replacing queued state. Never interrupts active work. */
    public void loadPendingTickets(String filepath) throws IOException {
        Path file = checkedPath(filepath);
        if (!Files.exists(file)) { return; }
        if (currentTicket != null) { throw new IOException("Cannot load while a ticket is active."); }
        Queue<Ticket> loaded = new Queue<>();
        List<Ticket> fileTickets = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        Ticket interrupted = null;
        try (PushbackReader reader = new PushbackReader(Files.newBufferedReader(file, StandardCharsets.UTF_8), 1)) {
            List<String> parts;
            while ((parts = readRecord(reader)) != null) {
                if (parts.size() != 5) { throw new IOException("Malformed ticket data."); }
                Ticket ticket;
                String status = parts.get(4).trim();
                try {
                    ticket = new Ticket(parts.get(0), parts.get(1), parts.get(2), parts.get(3), clock);
                    if (!status.equals("Open") && !status.equalsIgnoreCase("In Progress")) {
                        throw new IllegalArgumentException("Invalid pending ticket status.");
                    }
                }
                catch (IllegalArgumentException e) { throw new IOException("Invalid ticket data.", e); }
                if (ids.contains(ticket.getId())) { throw new IOException("Duplicate ticket ID."); }
                for (Ticket completed : completedTickets) {
                    if (completed.getId().equals(ticket.getId())) { throw new IOException("Duplicate completed ticket ID."); }
                }
                ids.add(ticket.getId());
                if (status.equalsIgnoreCase("In Progress")) {
                    if (interrupted != null) { throw new IOException("Multiple active tickets in save file."); }
                    interrupted = ticket;
                }
                else { fileTickets.add(ticket); }
            }
        }
        // Also accepts Vivaan's older format, which wrote the active record last.
        if (interrupted != null) { loaded.enqueue(interrupted); }
        for (Ticket ticket : fileTickets) { loaded.enqueue(ticket); }
        openTickets = loaded;
    }

    /** Current active ticket, or null if none. */
    public Ticket getCurrentTicket() { return currentTicket; }

    /** Average completed processing seconds, or -1 if no samples exist. */
    public long getAverageResolutionTime() { return timing.getAverageResolutionTime(); }

    /** Pending-ticket wait in seconds; validates ID even if timing is unavailable. */
    public long getEstimatedWaitTime(String id) {
        Long elapsed = currentTicket == null ? null : currentTicket.getElapsedProcessingSeconds();
        return timing.getEstimatedWaitTime(getPendingTickets(), id, elapsed);
    }

    /** Compatibility driver operation; the user menu uses start and complete separately. */
    public Ticket processNextTicket() {
        startNextTicket();
        return completeCurrentTicket();
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /** Reads quoted CSV, including commas, quotes, and newlines in 
     *  descriptions. 
     */
    private static List<String> readRecord(PushbackReader reader) 
        throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean afterQuote = false;
        boolean seen = false;
        int code;
        while ((code = reader.read()) != -1) {
            seen = true;
            char ch = (char)code;
            if (quoted) {
                if (ch == '"') {
                    int next = reader.read();
                    if (next == '"') {
                        field.append('"');
                    }
                    else {
                        quoted = false;
                        afterQuote = true;
                        if (next != -1) { reader.unread(next); }
                    }
                }
                else { field.append(ch); }
            }
            else if (ch == ',') {
                fields.add(field.toString());
                field.setLength(0);
                afterQuote = false;
            }
            else if (ch == '\n' || ch == '\r') {
                if (ch == '\r') {
                    int next = reader.read();
                    if (next != -1 && next != '\n') { reader.unread(next); }
                }
                fields.add(field.toString());
                return fields;
            }
            else if (ch == '"' && field.length() == 0 && !afterQuote) {
                quoted = true;
            }
            else if (ch == '"' || (afterQuote && !Character.isWhitespace(ch))) {
                throw new IOException("Malformed CSV field.");
            }
            else if (!afterQuote) { field.append(ch); }
        }
        if (quoted) { throw new IOException("Unclosed CSV quote."); }
        if (!seen) { return null; }
        fields.add(field.toString());
        return fields;
    }

}
