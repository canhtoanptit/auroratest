package org.example.aurora;

/**
 * Represents an account with an ID and a balance.
 */
public record Account(long id, long balance) {
    /**
     * Constructs an Account object with the given ID and balance.
     *
     * @param id      the ID of the account
     * @param balance the balance of the account
     */
    public Account {
    }

    /**
     * Retrieves the ID of the account.
     *
     * @return the ID of the account
     */
    @Override
    public long id() {
        return this.id;
    }

    /**
     * Retrieves the balance of the account.
     *
     * @return the balance of the account
     */
    @Override
    public long balance() {
        return this.balance;
    }

}
