package org.example.aurora;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static org.example.aurora.AccountTestHelper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

/**
 * Unit test for aurora.AccountCacheImpl
 */
public class AccountCacheTest {
    private AccountCacheExtendForTest accountCache;
    private Consumer<Account> accountListenerMock;
    private final Random random = new Random();
    private final ReentrantReadWriteLock updateLock = new ReentrantReadWriteLock();

    @BeforeEach
    public void setUp() {
        accountCache = new AccountCacheExtendForTest(5);
        accountListenerMock = mock(Consumer.class);
        accountCache.subscribeForAccountUpdates(accountListenerMock);
    }

    @Test
    void GivenExistingAccount_WhenUpdateAccount_ThenReturnAccountWithListener() {
        accountCache.subscribeForAccountUpdates(System.out::println);
        accountCache.subscribeForAccountUpdates(account -> System.out.println("process sub 2 " + account));
        accountCache.subscribeForAccountUpdates(account -> {
            try {
                var r = random.nextInt(1000) + 200;
                System.out.println(r);
                Thread.sleep(r);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Sleep in sub 3 " + account);
        });
        Account account1 = new Account(1, 1000);
        accountCache.putAccount(account1);

        Account account2 = new Account(1, 2000);
        accountCache.putAccount(account2);

        assertEquals(account2, accountCache.getAccountById(1));
    }

    @Test
    void GivenAccountID_WhenGetAccountByID_ThenReturnAccount() {
        Account account = new Account(1, 1000);
        accountCache.putAccount(account);
        assertEquals(account, accountCache.getAccountById(1));
    }

    @Test
    void GivenEmptyCache_WhenGetNonExistentAccountById_ThenReturnNull() {
        assertNull(accountCache.getAccountById(999));
    }

    @Test
    void Given2AccountsToCache_WhenGetTop3AccountsByBalance_ThenReturn2Accounts() {
        Account account1 = new Account(1, 1000);
        Account account2 = new Account(2, 2000);
        accountCache.putAccount(account1);
        accountCache.putAccount(account2);

        assertEquals(2, accountCache.getTop3AccountsByBalance().size());
    }

    @Test
    void Given5AccountToCache_WhenGetTop3AccountsByBalance_ThenReturn3AccountOrderByBalance() {
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
    public void GivenEmptyCache_WhenGetTop3AccountsByBalance_ThenReturnEmptyList() {
        assertEquals(0, accountCache.getTop3AccountsByBalance().size());
    }

    @Test
    public void GivenCacheWithAccountHasHit_WhenGetAccountByIdHitCount_ThenReturnTotalHitCount() {
        Account account = new Account(1, 1000);
        accountCache.putAccount(account);
        for (int i = 1; i <= 100; i++) {
            accountCache.getAccountById(1);
            assertEquals(i, accountCache.getAccountByIdHitCount());
        }
    }

    @Test
    public void GivenCacheWithAccountDoesNotHit_WhenGetAccountByIdHitCount_ThenReturnZero() {
        Account account = new Account(1, 1000);
        accountCache.putAccount(account);
        assertEquals(0, accountCache.getAccountByIdHitCount());
    }

    @Test
    public void GivenCache_WhenRandomPutAndGet_ThenReturnCorrespondingAccount() {
        for (int i = 0; i < 100; i++) {
            Account account = new Account(i, generateRandomBalance());
            accountCache.putAccount(account);
            if (i > 0) {
                accountCache.getAccountById(random.nextInt(i));
                Account accountUpdate = new Account(random.nextInt(i), generateRandomBalance());
                accountCache.putAccount(accountUpdate);
            }
            assertTop3AccountsAreCorrect(
                    accountCache.getTop3AccountsByBalance(),
                    new ArrayList<>(this.accountCache.getAllAccounts())
            );
            assertEquals(account, accountCache.getAccountById(i));
        }
    }

    @Test
    public void GivenAccountCache_WhenMultiThreadedPutAndGet_ThenReturnCorrespondingResult() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(50);
        AtomicInteger counter = new AtomicInteger();
        accountCache.subscribeForAccountUpdates(acc -> System.out.println("process sub 1 " + acc));
        accountCache.subscribeForAccountUpdates(acc -> {
            try {
                var r = random.nextInt(1000) + 200;
                System.out.println(r);
                Thread.sleep(r);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Sleep in sub 2 " + acc);
            counter.getAndIncrement();
        });
        List<Callable<AccountTestHelper.Result>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
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
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> counter.get() == 10);
    }

    /**
     * Compare both countdown lack with awaitdility
     */
    @Test
    void GivenExistingAccount_WhenMultiThreadUpdateAccount_ThenReturnAccount() {
        accountCache.subscribeForAccountUpdates(System.out::println);
        accountCache.subscribeForAccountUpdates(account -> System.out.println("process sub 2 " + account));
        accountCache.subscribeForAccountUpdates(account -> {
            try {
                var r = random.nextInt(1000) + 200;
                System.out.println(r);
                Thread.sleep(r);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Sleep in sub 3 " + account);
        });
        //CountDownLatch countDownLatch = new CountDownLatch(2);
        new Thread(() -> {
            Account account1 = new Account(1, 1000);
            accountCache.putAccount(account1);
            //countDownLatch.countDown();
        }).start();

        new Thread(() -> {
            Account account2 = new Account(1, 2000);
            accountCache.putAccount(account2);
            assertEquals(account2, accountCache.getAccountById(1));
            //countDownLatch.countDown();
        }).start();
        //countDownLatch.await();
        Awaitility.await().atLeast(5, TimeUnit.SECONDS).until(() -> {
            Thread.sleep(5_000);
            return true;
        });
    }

    private Callable<AccountTestHelper.Result> getResultCallable(int i) {
        return () -> {
            Account account = new Account(i, generateRandomBalance());
            updateLock.writeLock().lock();
            try {
                accountCache.putAccount(account);
                if (i > 0) {
                    accountCache.getAccountById(random.nextInt(i));
                    Account accountUpdate = new Account(random.nextInt(i), generateRandomBalance());
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
}
