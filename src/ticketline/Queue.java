package ticketline;

import java.util.List;
import java.util.ArrayList;

// -------------------------------------------------------------------------
/**
 *  A linked chain queue class that prioritizes tickets of higher severity level
 * 
 *  @author vivaandutt
 *  @version Nov 8, 2026
 */
public class Queue<T extends Comparable<T>> {
    
    private Node<T> frontNode;
    private Node<T> backNode;
    private int size;
    
    // ----------------------------------------------------------
    /**
     * Create a new Queue object.
     */
    public Queue() {
        frontNode = null;
        backNode = null;
        size = 0;
    }
    
    // ----------------------------------------------------------
    /**
     * Adds a new object to the queue
     * @param ticket the object to be added
     * @throws IllegalArgumentException if ticket is null
     */
    public void enqueue(T ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("Cannot enqueue a null object.");
        }
        
        Node<T> newNode = new Node<>(ticket);
        
        if (isEmpty()) {
            frontNode = newNode;
            backNode = newNode;
        } else if (ticket.compareTo(frontNode.data) < 0) {
            newNode.setNext(frontNode);
            frontNode = newNode;
        } else {
            Node<T> current = frontNode;
            
            while (current.getNext() != null && 
                current.getNext().getData().compareTo(ticket) <= 0) {
                current = current.getNext();
            }
            
            newNode.setNext(current.getNext());
            current.setNext(newNode);
            
            if (newNode.getNext() == null) {
                backNode = newNode;
            }
        }
        
        size++;
    }
    
    // ----------------------------------------------------------
    /**
     * Removes and returns the first item in queue.
     * @return the first item in queue
     * @throws IllegalStateException if queue is empty
     */
    public T dequeue() {
        
        if (isEmpty()) {
            throw new IllegalStateException("Queue is empty.");
        }
        
        T ticket = frontNode.getData();
        
        frontNode = frontNode.getNext();
        size--;
        
        if (frontNode == null) {
            backNode = null;
        }
        
        return ticket;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the first item in the queue without removing it.
     * @return the first item in the queue
     * @throws IllegalStateException if the queue is empty
     */
    public T peek() {
        
        if (isEmpty()) {
            throw new IllegalStateException("Queue is empty.");
        }
        
        return frontNode.getData();
    }
    
    // ----------------------------------------------------------
    /**
     * Checks whether the queue has items in it or not.
     * @return true if the queue has no items, false otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the number of items in the queue.
     * @return size
     */
    public int size() {
        return size;
    }
    
    // ----------------------------------------------------------
    /**
     * Returns the queue as a list.
     * @return a list with all the entries in the queue
     */
    public List<T> toList() {
        
        List<T> result = new ArrayList<T>();
        Node<T> current = frontNode;
        
        while (current != null) {
            result.add(current.getData());
            current = current.getNext();
        }
        
        return result;
    }
    
    
    private static class Node<T> {
        
        private T data;
        private Node<T> next;
        
        public Node(T data) {
            this.data = data;
            this.next = null;
        }
        
        public T getData() {
            return data;
        }
        
        public Node<T> getNext() {
            return next;
        }
        
        public void setNext(Node<T> newNext) {
            this.next = newNext;
        }
    }
}
