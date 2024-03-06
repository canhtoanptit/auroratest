package org.example.aurora;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Help method for account test
 */
public class AccountTestHelper {
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

    public static void assertTop3AccountsAreCorrect(List<Account> actualTop3Accounts, List<Account> allAccounts) {
        List<Account> expectedTop3Accounts = getExpectedTop3Accounts(allAccounts);
        assertEquals(expectedTop3Accounts.size(), actualTop3Accounts.size());
        for (int i = 0; i < expectedTop3Accounts.size(); i++) {
            assertEquals(expectedTop3Accounts.get(i), actualTop3Accounts.get(i));
        }
    }

    private static List<Account> getExpectedTop3Accounts(List<Account> allAccounts) {
        allAccounts.sort(Comparator.comparingLong(Account::balance).reversed());
        return allAccounts.subList(0, Math.min(3, allAccounts.size()));
    }
}
