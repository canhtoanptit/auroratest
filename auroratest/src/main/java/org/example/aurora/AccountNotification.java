package org.example.aurora;

import java.util.concurrent.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;

public class AccountNotification {
    private final CopyOnWriteArrayList<Consumer<Account>> accountListeners = new CopyOnWriteArrayList<>();
    ExecutorService notifierService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final StampedLock notificationLock = new StampedLock();

    public void notify(Account account) {
        notifierService.execute(() -> {
            var stamp = notificationLock.writeLock();
            try {
                accountListeners.forEach(listener -> listener.accept(account));
            } finally {
                notificationLock.unlockWrite(stamp);
            }
        });
    }

    public void addListener(Consumer<Account> listener) {
        this.accountListeners.add(listener);
    }
}
