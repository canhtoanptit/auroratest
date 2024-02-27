package org.example.aurora;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class AccountCacheImpl implements AccountCache {
    private final Map<Long, Account> cacheMap;
    private Consumer<Account> accountListener;
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ExecutorService listenerThreadPool = Executors.newCachedThreadPool();

    private final int capacity;
    private int countGetByIdHit;

    public AccountCacheImpl(int capacity) {
        this.capacity = capacity;
        this.cacheMap = new LinkedHashMap<>(capacity, 0.75f, true)  {
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
            this.accountListener = listener;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<Account> getTop3AccountsByBalance() {
        lock.readLock().lock();
        try {
            Account max1 = null, max2 = null, max3 = null;
            for (Account acc : cacheMap.values()) {
                if (max1 == null || acc.balance > max1.balance) {
                    max3 = max2;
                    max2 = max1;
                    max1 = acc;
                } else if (max2 == null || acc.balance > max2.balance) {
                    max3 = max2;
                    max2 = acc;
                } else if (max3 == null || acc.balance > max3.balance) {
                    max3 = acc;
                }
            }
            List<Account> result = new ArrayList<>();
            if (max1 != null) result.add(new Account(max1.getId(), max1.getBalance()));
            if (max2 != null) result.add(new Account(max2.getId(), max2.getBalance()));
            if (max3 != null) result.add(new Account(max3.getId(), max3.getBalance()));
            return result;
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
        if (this.accountListener != null) {
            Account accountForSubscribe = new Account(account.id, account.getBalance());
            this.listenerThreadPool.execute(() -> this.accountListener.accept(accountForSubscribe));
        }
    }
}
