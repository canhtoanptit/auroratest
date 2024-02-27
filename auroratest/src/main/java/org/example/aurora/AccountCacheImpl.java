package org.example.aurora;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * An implementation of {@link AccountCache}
 */
public class AccountCacheImpl implements AccountCache {
    protected final Map<Long, Account> cacheMap;
    private final CopyOnWriteArrayList<Consumer<Account>> accountListeners = new CopyOnWriteArrayList<>();
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ExecutorService listenerThreadPool = Executors.
            newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final int capacity;
    private int countGetByIdHit;

    public AccountCacheImpl(int capacity) {
        this.capacity = capacity;
        this.cacheMap = new LinkedHashMap<>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<Long, Account> eldest) {
                return size() > AccountCacheImpl.this.capacity;
            }
        };
    }

    @Override
    public Account getAccountById(long id) {
        lock.writeLock().lock();
        try {
            Account account = cacheMap.get(id);
            if (account == null) {
                return null;
            }
            this.countGetByIdHit++;
            return new Account(account.id, account.getBalance());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void subscribeForAccountUpdates(Consumer<Account> listener) {
        lock.writeLock().lock();
        try {
            this.accountListeners.add(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<Account> getTop3AccountsByBalance() {
        lock.readLock().lock();
        try {
            return cacheMap.values().stream()
                    .sorted(Comparator.comparingLong(Account::getBalance).reversed())
                    .limit(3)
                    .map(acc -> new Account(acc.getId(), acc.getBalance()))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getAccountByIdHitCount() {
        lock.readLock().lock();
        try {
            return this.countGetByIdHit;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void putAccount(Account account) {
        lock.writeLock().lock();
        try {
            Account accountCopy = new Account(account.id, account.getBalance());
            cacheMap.put(accountCopy.id, accountCopy);
        } finally {
            lock.writeLock().unlock();
        }
        for (Consumer<Account> listener : this.accountListeners) {
            if (listener != null) {
                Account accountForSubscribe = new Account(account.id, account.getBalance());
                this.listenerThreadPool.execute(() -> listener.accept(accountForSubscribe));
            }
        }
    }
}
