## Account Cache Implementation

### Overview
This repository contains an implementation of an account cache (`AccountCacheImpl`) in Java, along with associated test 
cases (`AccountCacheTest`). The account cache is designed to store account information and provide methods for accessing and updating accounts.

### Account Cache Implementation (`AccountCacheImpl`)
The `AccountCacheImpl` class implements the `AccountCache` interface and provides the following functionality:
- Caching of account objects based on their IDs.
- Subscription mechanism for receiving updates about account changes.
- Retrieval of the top 3 accounts by balance.
- Tracking the number of times an account is accessed by its ID.

### How to Run Test Cases
To run the test cases for the `AccountCacheImpl` class, follow these steps:

1. Clone this repository to your local machine.
2. Open the project in your preferred Java development environment.
3. Navigate to the `src/test/java/aurora` directory.
4. Open the `AccountCacheTest.java` file.
5. Run the test cases using your IDE's test runner or by executing the Maven command `mvn test`.

### Test Cases Description
The `AccountCacheTest` class contains test cases to ensure the correctness of the `AccountCacheImpl` implementation.
These test cases cover various scenarios, including:
- Adding new accounts to the cache.
- Retrieving accounts by ID.
- Verifying account listener functionality.
- Testing multi-threaded access to the cache.
- Checking the accuracy of the top 3 accounts by balance.

### Additional Notes
- Ensure that you have Java and Maven installed on your system to run the test cases.
- Review the test case results to verify the correctness of the `AccountCacheImpl` implementation.
- Modify or extend the test cases as needed to cover additional scenarios or edge cases.

This README provides an overview of the `AccountCacheImpl` implementation and instructions for running the associated test cases.
For more detailed information, refer to the source code and comments in the `AccountCacheImpl` and `AccountCacheTest` classes.