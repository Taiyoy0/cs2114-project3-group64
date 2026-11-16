package ticketline;

import java.time.Instant;
import java.time.Duration;
import java.time.Clock;

// -------------------------------------------------------------------------
/**
 *  Creates a ticket object.
 * 
 *  @author vivaandutt
 *  @version Nov 8, 2026
 */
public class Ticket implements Comparable<Ticket> {
    
    private String id;
    private String category;
    private String description;
    private String status;
    private String severity;
    private Instant startTime;
    private Instant completionTime;
    private Clock clock;
    
    // ----------------------------------------------------------
    /**
     * Create a new Ticket object.
     * @param i the id of the ticket.
     * @param c the category of the ticket.
     * @param d the description of the ticket.
     * @param s the severity of the ticket.
     * @throws IllegalArgumentException if i, c, d, and/or s are null or empty
     */
    public Ticket(String i, String c, String d, String s) {
        this(i, c, d, s, Clock.systemUTC());
    }
    
    // ----------------------------------------------------------
    /**
     * Create a new Ticket object.
     * @param i the id of the ticket.
     * @param c the category of the ticket.
     * @param d the description of the ticket.
     * @param s the severity of the ticket.
     * @param cl the clock used to measure ticket timestamps.
     * @throws IllegalArgumentException if i, c, d, s, and/or cl are invalid
     */
    public Ticket(String i, String c, String d, String s, Clock cl) {
        if (i == null || i.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid ID.");
        }
        
        if (c == null || c.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid category.");
        }
        
        if (d == null || d.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid description.");
        }
        
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid severity.");
        }
        if (cl == null) {
            throw new IllegalArgumentException("Clock cannot be null.");
        }
        
        id = i;
        category = c;
        description = d;
        severity = s;
        status = "Open";
        clock = cl;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the id of the ticket.
     * @return id
     */
    public String getId() {
        return id;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the category of the ticket.
     * @return category
     */
    public String getCategory() {
        return category;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the description of the ticket.
     * @return description
     */
    public String getDescription() {
        return description;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the severity of the ticket.
     * @return severity
     */
    public String getSeverity() {
        return severity;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the status of the ticket.
     * @return status
     */
    public String getStatus() {
        return status;
    }
    
    // ----------------------------------------------------------
    /**
     * Updates the description of the ticket.
     * @param newDesc the updated description for the ticket
     * @throws IllegalArgumentException if the new description is null or empty
     */
    public void updateDescription(String newDesc) {
        if(newDesc == null || newDesc.isEmpty()) {
            throw new IllegalArgumentException("Invalid description.");
        }
        
        description = newDesc;
    }
    
    // ----------------------------------------------------------
    /**
     * Updates the status of the ticket.
     * @param newStatus the updated status for the ticket
     * @throws IllegalArgumentException if the new status is null or empty
     */
    public void updateStatus(String newStatus) {
        if (newStatus == null || !(newStatus.equals("Open") || 
            newStatus.equals("In progress") || newStatus.equals("Closed"))) {
            throw new IllegalArgumentException("Invalid status.");
        }
        
        status = newStatus;
    }

    // ----------------------------------------------------------
    /**
     * Compares two ticket objects based on severity.
     * @param o the other ticket being compared to
     * @throws NullPointerException if the other ticket is null
     */
    @Override
    public int compareTo(Ticket o) {
        if (o == null) {
            throw new NullPointerException();
        }
        
        return Integer.compare(getSeverityValue(o.severity), 
            getSeverityValue(this.severity));
    }
    
    // ----------------------------------------------------------
    /**
     * Gets the severity value of a ticket as an int
     * @param sev the severity field of the ticket
     * @throws IllegalArgumentException if the severity isn't "low", "medium",
     * "high", or "critical"
     */
    private int getSeverityValue(String sev) {
        switch (sev.trim().toLowerCase()) {
            case "low":
                return 1;
            case "medium":
                return 2;
            case "high":
                return 3;
            case "critical":
                return 4;
            default:
                throw new IllegalArgumentException("Invalid severity.");
        }
    }
    
    // ----------------------------------------------------------
    /**
     * Starts processing the ticket.
     * @throws IllegalStateException if the ticket has already begun/finished
     * processing
     */
    public void startProcessing() {
        if (completionTime != null) {
            throw new IllegalStateException
            ("Ticket has already been processed.");
        }
        if (startTime != null) {
            throw new IllegalStateException
            ("Ticket is already being processed.");
        }
        
        startTime = Instant.now(clock);
        updateStatus("In progress");
    }
    
    // ----------------------------------------------------------
    /**
     * Finishes processing the ticket.
     * @throws IllegalStateException if the ticket hasn't begun processing or
     * if it has already finished
     */
    public void completeProcessing() {
        if (startTime == null) {
            throw new IllegalStateException
            ("Ticket must start processing first.");
        }
        if (completionTime != null) {
            throw new IllegalStateException
            ("Ticket has already finished processing.");
        }
        
        completionTime = Instant.now(clock);
        updateStatus("Closed");
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the time elapsed between the ticket processing began and when it
     * finished.
     * @return duration between startTime and completionTime
     * @throws IllegalStateException if startTime and/or completionTime are null
     */
    public Duration getResolutionDuration() {
        if (startTime == null) {
            throw new IllegalStateException
            ("Ticket must start processing first.");
        }
        if (completionTime == null) {
            throw new IllegalStateException
            ("Ticket must finish processing first.");
        }
        
        return Duration.between(startTime, completionTime);
    }
}
