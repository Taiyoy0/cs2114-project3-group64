package ticketline;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

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
    private final Clock clock;
    private Instant startTime;
    private Instant completionTime;
    
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

    /** Uses an injectable clock so timing tests need no real waiting. */
    public Ticket(String i, String c, String d, String s, Clock cl) {
        if (cl == null) { throw new IllegalArgumentException("Clock cannot be null."); }
        clock = cl;
        
        id = normalizeId(i);
        category = requireText(c, "category");
        if (!(category.equalsIgnoreCase("Hardware")
            || category.equalsIgnoreCase("Software")
            || category.equalsIgnoreCase("Network")
            || category.equalsIgnoreCase("Account")
            || category.equalsIgnoreCase("Other"))) {
            throw new IllegalArgumentException("Choose Hardware, Software, Network, Account, or Other.");
        }
        description = requireText(d, "description");
        severity = requireText(s, "severity");
        getSeverityValue(severity);
        status = "Open";
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
        description = requireText(newDesc, "description");
    }
    
    // ----------------------------------------------------------
    /**
     * Updates the status of the ticket.
     * @param newStatus the updated status for the ticket
     * @throws IllegalArgumentException if the new status is null or empty
     */
    public void updateStatus(String newStatus) {
        String value = requireText(newStatus, "status");
        if (!value.equals("Open") && !value.equals("In Progress")
            && !value.equals("Closed")) {
            throw new IllegalArgumentException("Invalid status.");
        }
        status = value;
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
        switch (sev.toLowerCase(java.util.Locale.ROOT)) {
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
    /** Normalizes a user ID so 001 and 1 identify the same ticket. */
    static String normalizeId(String value) {
        try {
            int number = Integer.parseInt(requireText(value, "ID"));
            if (number > 0) {
                return String.valueOf(number);
            }
        }
        catch (NumberFormatException e) {
            // Give the caller the same validation error for every invalid ID.
        }
        throw new IllegalArgumentException("Ticket ID must be a positive whole number.");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid " + field + ".");
        }
        return value.trim();
    }

    /** Begins Vivaan's ticket lifecycle; a ticket may start only once. */
    public void startProcessing() {
        if (startTime != null || !"Open".equals(status)) {
            throw new IllegalStateException("Ticket has already started or finished.");
        }
        startTime = clock.instant();
        updateStatus("In Progress");
    }

    /** Completes processing; a backward clock is rejected without closing it. */
    public void completeProcessing() {
        if (startTime == null || completionTime != null
            || !"In Progress".equals(status)) {
            throw new IllegalStateException("Ticket must be active to complete.");
        }
        Instant now = clock.instant();
        if (now.isBefore(startTime)) {
            throw new IllegalStateException("Clock moved before processing started.");
        }
        completionTime = now;
        updateStatus("Closed");
    }

    /** Returns duration only after the ticket has successfully completed. */
    public Duration getResolutionDuration() {
        if (startTime == null || completionTime == null) {
            throw new IllegalStateException("Ticket must finish processing first.");
        }
        return Duration.between(startTime, completionTime);
    }

    /** Returns elapsed whole seconds, clamped to zero if the clock moves back. */
    public long getElapsedProcessingSeconds() {
        if (startTime == null) {
            throw new IllegalStateException("Ticket must start processing first.");
        }
        Instant end = completionTime == null ? clock.instant() : completionTime;
        return Math.max(0, Duration.between(startTime, end).getSeconds());
    }
}
