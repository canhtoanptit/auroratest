package org.example.aurora;

import java.util.Collection;

public class AccountCacheExtendForTest extends AccountCacheImpl {

    public AccountCacheExtendForTest(int capacity) {
        super(capacity);
    }

    public Collection<Account> getAllAccounts() {
        lock.readLock().lock();
        try {
            return this.cacheMap.values();
        } finally {
            this.lock.readLock().unlock();
        }
    }
}

