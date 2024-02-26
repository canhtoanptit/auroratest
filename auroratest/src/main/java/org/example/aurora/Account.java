package org.example.aurora;

import java.util.Objects;

/**
 * Represents an account with an ID and a balance.
 */
public class Account {
    public Long id;
    public Long balance;

    /**
     * Constructs an Account object with the given ID and balance.
     *
     * @param id      the ID of the account
     * @param balance the balance of the account
     */
    public Account(long id, long balance) {
        this.id = id;
        this.balance = balance;
    }

    /**
     * Retrieves the ID of the account.
     *
     * @return the ID of the account
     */
    public long getId() {
        return this.id;
    }

    /**
     * Retrieves the balance of the account.
     *
     * @return the balance of the account
     */
    public long getBalance() {
        return this.balance;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Account other = (Account) obj;
        return id == other.id && balance == other.balance;
    }

    /**
     * Returns a hash code value for the account.
     *
     * @return a hash code value for this account
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, balance);
    }
}
