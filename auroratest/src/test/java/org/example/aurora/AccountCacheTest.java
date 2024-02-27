package org.example.aurora;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit test for aurora.AccountCacheImpl
 */
public class AccountCacheTest {
    private AccountCacheExtendForTest accountCache;
    private Consumer<Account> accountListenerMock;
    private Random random;
    private final Lock printLock = new ReentrantLock();
    private final ReentrantReadWriteLock updateLock = new ReentrantReadWriteLock();

    @BeforeEach
    public void setUp() {
        accountCache = new AccountCacheExtendForTest(5);
        accountListenerMock = mock(Consumer.class);
        accountCache.subscribeForAccountUpdates(accountListenerMock);
        random = new Random();
    }

    @Test
    void testUpdateExistingAccount() {
        Account account1 = new Account(1, 1000);
        accountCache.putAccount(account1);
        await().atMost(1, SECONDS).until(() -> {
            verify(accountListenerMock).accept(account1);
            return true;
        });

        Account account2 = new Account(1, 2000);
        accountCache.putAccount(account2);
        await().atMost(1, SECONDS).until(() -> {
            verify(accountListenerMock).accept(account2);
            return true;
        });

        assertEquals(account2, accountCache.getAccountById(1));
    }

    @Test
    void testGetAccountById() {
        Account account = new Account(1, 1000);
        accountCache.putAccount(account);
        assertEquals(account, accountCache.getAccountById(1));
    }

    @Test
    void testGetNonExistentAccountById() {
        assertNull(accountCache.getAccountById(999));
    }

    @Test
    void testGetTop3AccountsByBalanceLessThan3() {
        Account account1 = new Account(1, 1000);
        Account account2 = new Account(2, 2000);
        accountCache.putAccount(account1);
        accountCache.putAccount(account2);

        assertEquals(2, accountCache.getTop3AccountsByBalance().size());
    }

    @Test
    void testGetTop3AccountsByBalanceGreaterThan3() {
        Account account1 = new Account(1, 1000);
        Account account2 = new Account(2, 2000);
        Account account3 = new Account(3, 3000);
        Account account4 = new Account(4, 4000);
        Account account5 = new Account(5, 5000);
        accountCache.putAccount(account1);
        accountCache.putAccount(account2);
        accountCache.putAccount(account3);
        accountCache.putAccount(account4);
        accountCache.putAccount(account5);

        assertEquals(3, accountCache.getTop3AccountsByBalance().size());
    }

    @Test
    public void testGetTop3AccountsByBalanceWithNoAccounts() {
        assertEquals(0, accountCache.getTop3AccountsByBalance().size());
    }

    @Test
    public void testGetAccountByIdHitCountWithAccess() {
        Account account = new Account(1, 1000);
        accountCache.putAccount(account);
        for (int i = 1; i <= 100; i++) {
            accountCache.getAccountById(1);
            assertEquals(i, accountCache.getAccountByIdHitCount());
        }
    }

    @Test
    public void testGetAccountByIdHitCountWithNoAccess() {
        Account account = new Account(1, 1000);
        accountCache.putAccount(account);
        assertEquals(0, accountCache.getAccountByIdHitCount());
    }

    @Test
    public void testManyPutAndGet() {
        for (int i = 0; i < 100; i++) {
            Account account = new Account(i, this.generateRandomBalance());
            accountCache.putAccount(account);
            if (i > 0) {
                accountCache.getAccountById(random.nextInt(i));
                Account accountUpdate = new Account(random.nextInt(i), this.generateRandomBalance());
                accountCache.putAccount(accountUpdate);
            }
            assertTop3AccountsAreCorrect(
                    accountCache.getTop3AccountsByBalance(),
                    new ArrayList<Account>(this.accountCache.getAllAccounts())
            );
            assertEquals(account, accountCache.getAccountById(i));
        }
    }

    static class Result {
        int taskId;
        long time;
        List<Account> actualTop3Accounts;
        List<Account> allAccounts;

        public Result(int taskId, long time, List<Account> actualTop3Accounts, List<Account> allAccounts) {
            this.taskId = taskId;
            this.time = time;
            this.actualTop3Accounts = actualTop3Accounts;
            this.allAccounts = allAccounts;
        }
    }

    @Test
    public void testzMultiThreadedPutAndGet() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(50);
        List<Callable<Result>> tasks = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Callable<Result> task = getResultCallable(i);
            tasks.add(task);
        }
        List<Future<Result>> results = null;
        try {
            results = executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Shutdown ExecutorService
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }
        if (results != null) {
            for (Future<Result> result : results) {
                try {
                    Result res = result.get();
                    assertTop3AccountsAreCorrect(res.actualTop3Accounts, res.allAccounts);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Callable<Result> getResultCallable(int i) {
        return () -> {
            Account account = new Account(i, this.generateRandomBalance());
            updateLock.writeLock().lock();
            try {
                accountCache.putAccount(account);
                if (i > 0) {
                    accountCache.getAccountById(random.nextInt(i));
                    Account accountUpdate = new Account(random.nextInt(i), this.generateRandomBalance());
                    accountCache.putAccount(accountUpdate);
                }
            } finally {
                updateLock.writeLock().unlock();
            }

            List<Account> actualTop3Accounts;
            List<Account> allAccounts;
            updateLock.readLock().lock();
            try {
                actualTop3Accounts = accountCache.getTop3AccountsByBalance();
                allAccounts = new ArrayList<>(accountCache.getAllAccounts());
            } finally {
                updateLock.readLock().unlock();
            }
            return new Result(i, System.currentTimeMillis(), actualTop3Accounts, allAccounts);
        };
    }

    private int generateRandomBalance() {
        return random.nextInt(999001) + 1000;
    }

    private void printTop3Accounts(List<Account> expectedTop3Accounts, List<Account> actualTop3Accounts) {
        printLock.lock();
        try {
            System.out.println("Expected Top 3 Accounts:");
            for (Account entry : expectedTop3Accounts) {
                System.out.println("ID: " + entry.id + ", Balance: " + entry.getBalance());
            }

            System.out.println("Actual Top 3 Accounts:");
            for (Account account : actualTop3Accounts) {
                System.out.println("ID: " + account.id + ", Balance: " + account.getBalance());
            }
        } finally {
            printLock.unlock();
        }
    }

    private void assertTop3AccountsAreCorrect(List<Account> actualTop3Accounts, List<Account> allAccounts) {
        List<Account> expectedTop3Accounts = getExpectedTop3Accounts(allAccounts);
        printTop3Accounts(expectedTop3Accounts, actualTop3Accounts);
        assertEquals(expectedTop3Accounts.size(), actualTop3Accounts.size());
        for (int i = 0; i < expectedTop3Accounts.size(); i++) {
            assertEquals(expectedTop3Accounts.get(i), actualTop3Accounts.get(i));
        }
    }

    private List<Account> getExpectedTop3Accounts(List<Account> allAccounts) {
        allAccounts.sort(Comparator.comparingLong(Account::getBalance).reversed());
        return allAccounts.subList(0, Math.min(3, allAccounts.size()));
    }
}
