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
        
        if (i == null || i == "") {
            throw new IllegalArgumentException("Invalid ID.");
        }
        
        if (c == null || c == "") {
            throw new IllegalArgumentException("Invalid category.");
        }
        
        if (d == null || d == "") {
            throw new IllegalArgumentException("Invalid description");
        }
        
        if (s == null || s == "") {
            throw new IllegalArgumentException("Invalid severity");
        }
        
        id = i;
        category = c;
        description = d;
        severity = s;
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
    public String returnDescription() {
        return description;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the severity of the ticket.
     * @return severity
     */
    public String returnSeverity() {
        return severity;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the status of the ticket.
     * @return status
     */
    public String returnStatus() {
        return status;
    }
    
    // ----------------------------------------------------------
    /**
     * Updates the description of the ticket.
     * @param newDesc the updated description for the ticket
     * @throws IllegalArgumentException if the new description is null or empty
     */
    public void updateDescription(String newDesc) {
        if(newDesc == null || newDesc == "" ) {
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
        if (newStatus == null || newStatus == "") {
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
        switch (sev.toLowerCase()) {
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
}
