/**
 *
 */
package org.theseed.protein.tags;

/**
 * This is a simple counting object.  It contains nothing but a single integer value.
 * Unlike the Integer class, it is mutable and can be updated.
 *
 * @author Bruce Parrello
 *
 */
public class Count implements Comparable<Count> {

    // FIELDS
    /** count value */
    private int value;

    /**
     * Construct a blank, empty count.
     */
    public Count() {
        this.value = 0;
    }

    /**
     * Construct a counter with a specified starting value.
     *
     * @param count		starting value for the count
     */
    public Count(int count) {
        this.value = count;
    }

    /**
     * @return the count value
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Clear the count.
     */
    public void clear() {
        this.value = 0;
    }

    /**
     * Increment the count.
     */
    public void increment() {
        this.value++;
    }

    /**
     * Add another count and return the result.
     *
     * @param other		other count to add
     */
    public int add(Count other) {
        this.value += other.value;
        return this.value;
    }

    /**
     * Add an integer value and return the result.
     *
     * @param incr		value to add
     */
    public int add(int incr) {
        this.value += incr;
        return this.value;
    }

    @Override
    public int compareTo(Count o) {
        return this.value - o.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Count)) {
            return false;
        }
        Count other = (Count) obj;
        if (this.value != other.value) {
            return false;
        }
        return true;
    }

}
