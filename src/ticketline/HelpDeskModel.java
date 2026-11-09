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
        
        if (idExists(id)) {
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
        
        for (Ticket t : openTickets.toList()) {
            if (t.getId() == i) {
                return true;
            }
        }
        
        for (Ticket t : completedTickets) {
            if (t.getId() == i) {
                return true;
            }
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
     * Removes the oldest ticket from openTickets and add it to 
     * completedTickets.
     * @return the ticket being moved from openTickets to closedTickets
     */
    public Ticket processNextTicket() {
        
        Ticket t = openTickets.dequeue();
        
        t.updateStatus("Closed");
        
        completedTickets.add(t);
        
        return t;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns whether there are open tickets pending.
     * @return true if openTicket is not empty
     */
    public boolean hasPendingTickets() {
        return !openTickets.isEmpty();
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the pending tickets as a List.
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
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            for (Ticket t : openTickets.toList()) {
                writer.println(
                    t.getId() + ", " +
                    t.getCategory() + ", " +
                    t.getDescription() + ", " +
                    t.getSeverity() + ", " +
                    t.getStatus()
                );
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
        
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                
                if (parts.length != 5) {
                    throw new IOException("Malformed ticket data.");
                }
                
                Ticket t;
                
                try {
                    t = new Ticket(parts[0], parts[1], parts[2], parts[3]);

                    t.updateStatus(parts[4]);
                }
                catch (IllegalArgumentException e) {
                    throw new IOException("Invalid ticket data.", e);
                }
                
                if (loadedIds.contains(t.getId()) || idExists(t.getId())) {
                    throw new IOException("Duplicate ticket ID.");
                }
                
                loadedIds.add(t.getId());
                loadedTickets.enqueue(t);
            }
        }
        
        openTickets = loadedTickets;
    }
}
