package org.example.aurora;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * An implementation of {@link AccountCache}
 */
public class AccountCacheImpl implements AccountCache {
    protected final Map<Long, Account> cacheMap;
    protected final StampedLock lock = new StampedLock();
    private final int capacity;
    private final AtomicInteger countGetByIdHit = new AtomicInteger();

    private final AccountNotification accountNotification;

    public AccountCacheImpl(int capacity) {
        this.capacity = capacity;
        this.cacheMap = new LinkedHashMap<>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<Long, Account> eldest) {
                return size() > AccountCacheImpl.this.capacity;
            }
        };
        this.accountNotification = new AccountNotification();
    }

    @Override
    public Account getAccountById(long id) {
        /*
         * we can use less expensive lock with read lock and atomic integer for counter as wel
         */
        long stamp = lock.readLock();
        try {
            Account account = cacheMap.get(id);
            if (account == null) {
                return null;
            }
            this.countGetByIdHit.incrementAndGet();
            return account;
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public void subscribeForAccountUpdates(Consumer<Account> listener) {
        this.accountNotification.addListener(listener);
    }

    @Override
    public List<Account> getTop3AccountsByBalance() {
        long stamp = lock.readLock();
        try {
            return cacheMap.values().stream()
                    .sorted(Comparator.comparingLong(Account::balance).reversed())
                    .limit(3)
                    .collect(Collectors.toList());
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public int getAccountByIdHitCount() {
        // can use atomic integer for count git
        return this.countGetByIdHit.get();
    }

    @Override
    public void putAccount(Account account) {
        long stamp = lock.writeLock();
        try {
            cacheMap.put(account.id(), account);
        } finally {
            lock.unlockWrite(stamp);
        }
        this.accountNotification.notify(account);
    }
}
