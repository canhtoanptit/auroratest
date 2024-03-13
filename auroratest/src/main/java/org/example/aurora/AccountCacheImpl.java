package org.example.aurora;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private List<Account> top3Account = new ArrayList<>();

    private final AtomicBoolean hasUpdateAccount = new AtomicBoolean();

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
            if (!hasUpdateAccount.get()) {
                return this.top3Account;
            }
            this.top3Account = cacheMap.values().stream()
                    .sorted(Comparator.comparingLong(Account::balance).reversed())
                    .limit(3)
                    .collect(Collectors.toList());
            return this.top3Account;
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
            // check if new account does not effected to top 3
            cacheMap.put(account.id(), account);
        } finally {
            lock.unlockWrite(stamp);
        }
        hasUpdateAccount.set(true);
        this.accountNotification.notify(account);
    }
}
