package org.example.aurora;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Help method for account test
 */
public class AccountTestHelper {
    private static final Lock printLock = new ReentrantLock();
    private static final Random random = new Random();
    public static int generateRandomBalance() {
        return random.nextInt(999001) + 1000;
    }

    public static class Result {
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

    public static void printTop3Accounts(List<Account> expectedTop3Accounts, List<Account> actualTop3Accounts) {
        Collections.synchronizedMap(HashMap.newHashMap(1));
        printLock.lock();
        try {
            System.out.println("Expected Top 3 Accounts:");
            for (Account entry : expectedTop3Accounts) {
                System.out.println("ID: " + entry.getId() + ", Balance: " + entry.getBalance());
            }

            System.out.println("Actual Top 3 Accounts:");
            for (Account account : actualTop3Accounts) {
                System.out.println("ID: " + account.getId() + ", Balance: " + account.getBalance());
            }
        } finally {
            printLock.unlock();
        }
    }

    public static void assertTop3AccountsAreCorrect(List<Account> actualTop3Accounts, List<Account> allAccounts) {
        List<Account> expectedTop3Accounts = getExpectedTop3Accounts(allAccounts);
        printTop3Accounts(expectedTop3Accounts, actualTop3Accounts);
        assertEquals(expectedTop3Accounts.size(), actualTop3Accounts.size());
        for (int i = 0; i < expectedTop3Accounts.size(); i++) {
            assertEquals(expectedTop3Accounts.get(i), actualTop3Accounts.get(i));
        }
    }

    private static List<Account> getExpectedTop3Accounts(List<Account> allAccounts) {
        allAccounts.sort(Comparator.comparingLong(Account::getBalance).reversed());
        return allAccounts.subList(0, Math.min(3, allAccounts.size()));
    }
}
