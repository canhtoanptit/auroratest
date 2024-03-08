package org.example.aurora;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AccountNotification {
    private final CopyOnWriteArrayList<Consumer<Account>> accountListeners = new CopyOnWriteArrayList<>();
    ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public void notify(Account account) {
        List<CompletableFuture<Void>> futures =
                accountListeners.stream()
                        .map(
                                listener ->
                                        CompletableFuture.runAsync(() -> listener.accept(account), service))
                        .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public void addListener(Consumer<Account> listener) {
        this.accountListeners.add(listener);
    }
}
