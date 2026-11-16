package ticketline;

import java.util.List;
import java.util.ArrayList;
import java.io.*;

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
    
    // ----------------------------------------------------------
    /**
     * Create a new HelpDeskModel object.
     */
    public HelpDeskModel() {
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
        Ticket newTicket = new Ticket(id, category, description, severity);
        
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
    public void startNextTicket() {
        
        if (currentTicket != null) {
            throw new IllegalStateException("Already processing a ticket.");
        }
        if (openTickets.isEmpty()) {
            throw new IllegalStateException("No tickets to process.");
        }
        
        currentTicket = openTickets.dequeue();
        currentTicket.startProcessing();
    }
    
    // ----------------------------------------------------------
    /**
     * Finishes processing the current ticket being processed.
     * @throws IllegalStateException if no ticket is currently being processed
     */
    public void completeCurrentTicket() {
        if (currentTicket == null) {
            throw new IllegalStateException
            ("No ticket currently being processed");
        }
        
        currentTicket.completeProcessing();
        completedTickets.add(currentTicket);
        currentTicket = null;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns whether there are open tickets pending. Considers currentTicket
     * as well.
     * @return true if openTicket is not empty
     */
    public boolean hasPendingTickets() {
        return !openTickets.isEmpty() || currentTicket != null;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the pending tickets as a List. Includes the currentTicket being
     * processed if there is one.
     * @return the entries of openTickets in a list
     */
    public List<Ticket> getPendingTickets() {
        ArrayList<Ticket> ret = new ArrayList<>(openTickets.toList());
        if (currentTicket != null) {
            ret.add(currentTicket);
        }
        return ret;
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
        
        try (BufferedWriter writer = new BufferedWriter
            (new FileWriter(filepath))) {
            for (Ticket t : openTickets.toList()) {
                writer.write(csv(t.getId()) + "," + csv(t.getCategory()) + ","
                    + csv(t.getDescription()) + "," + csv(t.getSeverity())
                    + "," + csv(t.getStatus()));
                writer.newLine();
            }
            if (currentTicket != null) {
                writer.write(csv(currentTicket.getId()) + "," 
                    + csv(currentTicket.getCategory()) + ","
                    + csv(currentTicket.getDescription()) + ","
                    + csv(currentTicket.getSeverity()) + ","
                    + csv(currentTicket.getStatus()));
                writer.newLine();
            }
        }
    }
    
    // ----------------------------------------------------------
    /**
     * Loads the pending tickets from a file and saves that to openTickets.
     * @param filepath the file storing the pending tickets
     * @throws IOException
     */
    public void loadPendingTickets(String filepath) throws IOException {
        
        File f = new File(filepath);
        
        if(!f.exists()) {
            return;
        }
        
        Queue<Ticket> loadedTickets = new Queue<>();
        List<String> loadedIds = new ArrayList<>();
        Ticket loadedCurrentTicket = null;
        
        try (PushbackReader reader = new PushbackReader(new FileReader(f), 1)) {
            List<String> parts;
            while ((parts = readRecord(reader)) != null) {
                if (parts.size() != 5) {
                    throw new IOException("Malformed ticket data.");
                }
                Ticket t;
                String status;
                
                try {
                    t = new Ticket(parts.get(0), parts.get(1), parts.get(2), 
                        parts.get(3));
                    status = parts.get(4).trim();

                    if (!status.equals("Open") && 
                        !status.equals("In progress")) {
                        throw new IllegalArgumentException
                        ("Invalid ticket status.");
                    }

                    t.updateStatus(status);
                }
                catch (IllegalArgumentException e) {
                    throw new IOException("Invalid ticket data.", e);
                }
                
                boolean completedId = false;

                for (Ticket completed : completedTickets) {
                    completedId |= completed.getId().equals(t.getId());
                }

                boolean currentId = currentTicket != null
                    && currentTicket.getId().equals(t.getId());

                if (loadedIds.contains(t.getId()) || completedId || currentId) {
                    throw new IOException("Duplicate ticket ID.");
                }
                
                loadedIds.add(t.getId());

                if (status.equals("Open")) {
                    loadedTickets.enqueue(t);
                } else {
                    if (loadedCurrentTicket != null) {
                        throw new IOException
                        ("Multiple tickets currently in progress.");
                    }

                    loadedCurrentTicket = t;
                }
            }
        }

        openTickets = loadedTickets;
        currentTicket = loadedCurrentTicket;
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
