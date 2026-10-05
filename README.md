# spring-transaction-demo
> Spring `@Transactional` Propagation Levels

## Overview

In Spring Framework, the `propagation` attribute of `@Transactional` defines how a transactional method behaves when it is called from another method that may already have an active transaction.

```
@Transactional(propagation = Propagation.REQUIRED)
public void processOrder() {
    // business logic
}
```
## Points to remember:
> - We can call normal method inside a transactional method of the same class
> - We can call another transactional method inside a transactional method of the same class
> - We can call transactional method inside a normal method of the same class but transaction won't trigger. Because @Transactional works when the method is invoked through the Spring proxy, not when another method in the same class directly calls it.

Propagation determines whether the method:

- Joins an existing transaction
- Creates a new transaction
- Executes without a transaction
- Suspends an existing transaction
- Throws an exception when a transaction is or is not present

## Test Data

| Propagation | Happy Case Data                           | Failure case data                                                  |
| --- |-------------------------------------------|--------------------------------------------------------------------|
| `REQUIRED` |                                   |                                                                    |
| `REQUIRES_NEW` |  |                                                                    |
| `MANDATORY` |                                   |                                                                    |
| `NEVER` |                           |                                                                    |
| `NOT_SUPPORTED` | {"productId": 3, "quantity": 1}| {"productId": 1, "quantity": 2} or {"productId": 3, "quantity": 2} |
| `SUPPORTS` |                                   |                                                                    |
| `NESTED` |     |                                                                    |

Spring provides the following propagation levels:

| Propagation | Existing Transaction | No Existing Transaction |
| --- | --- | --- |
| `REQUIRED` | Joins it | Creates a new transaction |
| `REQUIRES_NEW` | Suspends it and creates a new transaction | Creates a new transaction |
| `MANDATORY` | Joins it | Throws exception |
| `NEVER` | Throws exception | Executes without a transaction |
| `NOT_SUPPORTED` | Suspends it | Executes without a transaction |
| `SUPPORTS` | Joins it | Executes without a transaction |
| `NESTED` | Creates a nested transaction/savepoint | Creates a new transaction |

---

## 1\. `REQUIRED`

### Definition

`REQUIRED` is the **default propagation level**.

If a transaction already exists, the method joins the existing transaction. If no transaction exists, Spring creates a new transaction.

```
@Transactional(propagation = Propagation.REQUIRED)
public void createOrder() {
    // ...
}
```

### Behavior

```
Caller
  |
  |-- Transaction A
  |
  +--> Method A
         |
         +--> Method B (@Transactional REQUIRED)
                |
                +--> joins Transaction A
```

If `Method B` fails and the exception causes rollback, the **entire transaction can be rolled back**.

### Typical use case

Use `REQUIRED` for normal business operations where multiple service methods should participate in the same transaction.

```
@Transactional
public void placeOrder() {
    saveOrder();
    updateInventory();
    createPayment();
}
```

All three operations can participate in the same transaction.

### Key point

> `REQUIRED` = **Use the current transaction if one exists; otherwise create one.**

---

# 2\. `REQUIRES_NEW`

### Definition

`REQUIRES_NEW` always executes within a **new transaction**.

If an existing transaction is present, Spring suspends it temporarily and creates a new transaction.

```
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveAuditLog() {
    // ...
}
```

### Behavior

```
Transaction A
    |
    +--> Method A
          |
          +--> Method B (REQUIRES_NEW)
                  |
                  |-- Transaction A suspended
                  |
                  |-- Transaction B started
                  |
                  +-- Transaction B committed/rolled back
          |
          +-- Transaction A resumes
```

The inner transaction is independent of the outer transaction.

### Example

```
@Transactional
public void processPayment() {

    paymentRepository.save(payment);

    auditService.saveAudit();
}
```

```
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveAudit() {
    auditRepository.save(audit);
}
```

If the outer transaction later rolls back, the audit transaction may already have committed.

### Typical use cases

- Audit logging
- Independent status/history records
- Separate retry operations
- Operations that must commit independently of the parent transaction

### Important consideration

`REQUIRES_NEW` can require an additional database connection while the original transaction is suspended. Excessive use can therefore contribute to connection-pool exhaustion.

### Key point

> `REQUIRES_NEW` = **Always create an independent transaction.**

---

# 3\. `MANDATORY`

### To Know
> - It uses/participating in existing transaction, if existing doesn't exist it will throw and exception.
> - if existing transaction present to participate and an exception occurs inside it(i.e. inside MANDATORY transaction), it will roll back the whole transaction
#### Test it
> - Comment @Transactional in OrderProcessingService `placeAnOrder` method and see the behaviour
> - Test it with order amount more than 2000 and see the behaviour.
### Definition

`MANDATORY` requires an existing transaction.

If a transaction exists, the method joins it.

If no transaction exists, Spring throws an exception.

```
@Transactional(propagation = Propagation.MANDATORY)
public void updateOrder() {
    // ...
}
```

### Behavior

With a transaction:

```
Transaction A
    |
    +--> Method B (MANDATORY)
            |
            +--> joins Transaction A
```

Without a transaction:

```
No Transaction
    |
    +--> Method B (MANDATORY)
            |
            +--> Exception
```

### Typical use case

Use this when a method **must never be called outside an existing transaction**.

It can be useful for lower-level service/repository operations that require transactional context.

### Key point

> `MANDATORY` = **An existing transaction is required.**

---

# 4\. `NEVER`

### Definition

`NEVER` requires that no transaction exists.

If a transaction exists, Spring throws an exception.

```
@Transactional(propagation = Propagation.NEVER)
public void executeOperation() {
    // ...
}
```

### Behavior

Without a transaction:

```
No Transaction
    |
    +--> Method B (NEVER)
            |
            +--> Executes successfully
```

With a transaction:

```
Transaction A
    |
    +--> Method B (NEVER)
            |
            +--> Exception
```

### Typical use case

Use it when the operation must explicitly run outside a transaction.

This propagation level is relatively uncommon.

### Key point

> `NEVER` = **A transaction must not exist.**

---

# 5\. `SUPPORTS`

### Definition

`SUPPORTS` means:

- Join an existing transaction if one exists.
- Otherwise execute without a transaction.

```
@Transactional(propagation = Propagation.SUPPORTS)
public void findOrder(Long orderId) {
    // ...
}
```

### Behavior

With an existing transaction:

```
Transaction A
    |
    +--> Method B (SUPPORTS)
            |
            +--> joins Transaction A
```

Without an existing transaction:

```
No Transaction
    |
    +--> Method B (SUPPORTS)
            |
            +--> executes without transaction
```

### Typical use case

Useful for operations that can work correctly both inside and outside a transaction, commonly read-oriented operations.

### Key point

> `SUPPORTS` = **Use a transaction if available; otherwise don't use one.**

---

# 6\. `NOT_SUPPORTED`

### Definition

`NOT_SUPPORTED` means that the method should **not execute within a transaction**.

If a transaction already exists, Spring suspends it while the method executes.

```
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void performNonTransactionalOperation() {
    // ...
}
```

### Behavior

```
Transaction A
    |
    +--> Method B (NOT_SUPPORTED)
            |
            |-- Transaction A suspended
            |
            +-- Executes without transaction
            |
            +-- Transaction A resumes
```

### Typical use cases

Use it when an operation should explicitly run outside the current transaction.

For example:

- Long-running non-transactional processing
- Operations where holding a database transaction is undesirable
- External operations that should not participate in the database transaction

### Key point

> `NOT_SUPPORTED` = **Suspend the current transaction and execute without one.**

---

# 7\. `NESTED`

### Definition

`NESTED` executes within a nested transaction-like scope when an existing transaction is available.

Spring generally implements this using a **database savepoint**.

```
@Transactional(propagation = Propagation.NESTED)
public void processItem() {
    // ...
}
```

### Behavior

```
Transaction A
    |
    +--> Method A
          |
          +--> Savepoint created
          |
          +--> Method B (NESTED)
                  |
                  +--> Executes
                  |
                  +--> Failure
                        |
                        +--> Roll back to savepoint
          |
          +--> Transaction A continues
```

Unlike `REQUIRES_NEW`, the nested operation does **not normally get an entirely independent physical transaction**.

### Example

```
@Transactional
public void processOrders() {

    processOrder1();

    try {
        processOrder2();
    } catch (Exception e) {
        // continue processing
    }

    processOrder3();
}
```

```
@Transactional(propagation = Propagation.NESTED)
public void processOrder2() {
    // ...
}
```

If `processOrder2()` rolls back to its savepoint, the outer transaction can potentially continue.

### Important consideration

`NESTED` depends on transaction infrastructure that supports savepoints. Its behavior is therefore more dependent on the transaction manager and database configuration than `REQUIRED` or `REQUIRES_NEW`.

### Key point

> `NESTED` = **Use a savepoint within the existing transaction when supported.**

---

# Propagation Comparison

## `REQUIRED` vs `REQUIRES_NEW`

This is one of the most important distinctions.

### `REQUIRED`

```
Outer Transaction
      |
      +---- Inner Method
                |
                +---- Same Transaction
```

If the inner operation causes the transaction to roll back, the outer transaction is affected.

### `REQUIRES_NEW`

```
Outer Transaction
      |
      +---- suspended
      |
      +---- Inner Method
                |
                +---- New Transaction
      |
      +---- Outer Transaction resumes
```

The inner transaction can commit independently.

---

## `REQUIRES_NEW` vs `NESTED`

| Feature | `REQUIRES_NEW` | `NESTED` |
| --- | --- | --- |
| New transaction | Yes | No, normally uses savepoint |
| Outer transaction suspended | Yes | No |
| Independent commit | Yes | No |
| Uses savepoints | No | Yes |
| Outer transaction can continue after inner rollback | Yes | Yes, when rollback is to savepoint |
| Requires savepoint support | No | Yes |

---

# Practical Example

Consider an order-processing application:

```
@Service
public class OrderService {

    @Transactional
    public void placeOrder(Order order) {

        orderRepository.save(order);

        paymentService.processPayment(order);

        auditService.saveAudit(order);
    }
}
```

Payment processing might use the default `REQUIRED`:

```
@Transactional(propagation = Propagation.REQUIRED)
public void processPayment(Order order) {
    // Participates in the order transaction
}
```

An audit operation that must commit independently could use `REQUIRES_NEW`:

```
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveAudit(Order order) {
    auditRepository.save(createAudit(order));
}
```

The resulting flow is:

```
Order Transaction
       |
       +-- Save Order
       |
       +-- Process Payment
       |       |
       |       +-- Same transaction
       |
       +-- Save Audit
               |
               +-- New transaction
               |
               +-- Commit
       |
       +-- Commit/Rollback Order Transaction
```

---

# Important Spring Consideration: Self-Invocation

Propagation relies on Spring's transaction interception/proxy mechanism.

Consider:

```
@Service
public class OrderService {

    public void methodA() {
        methodB();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB() {
        // ...
    }
}
```

The call:

```
methodB();
```

is a **self-invocation**.

Because the call does not go through the Spring proxy, the transactional interception may not occur as expected. Consequently, `REQUIRES_NEW` may not actually create a new transaction.

A common solution is to move the transactional method to another Spring-managed service:

```
@Service
public class OrderService {

    private final AuditService auditService;

    public OrderService(AuditService auditService) {
        this.auditService = auditService;
    }

    public void methodA() {
        auditService.methodB();
    }
}
```

```
@Service
public class AuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB() {
        // ...
    }
}
```

---

# Quick Reference

| Level | Existing Transaction | No Transaction | Common Use |
| --- | --- | --- | --- |
| `REQUIRED` | Join | Create | Default business transaction |
| `REQUIRES_NEW` | Suspend + create new | Create | Independent audit/logging |
| `SUPPORTS` | Join | No transaction | Optional transaction |
| `NOT_SUPPORTED` | Suspend | No transaction | Force non-transactional execution |
| `MANDATORY` | Join | Exception | Require caller transaction |
| `NEVER` | Exception | No transaction | Require no transaction |
| `NESTED` | Savepoint | Create | Partial rollback within transaction |

---

# Recommended Usage

For most Spring applications:

- Use **`REQUIRED`** as the default for normal transactional business operations.
- Use **`REQUIRES_NEW`** when an operation must commit or roll back independently.
- Use **`SUPPORTS`** when the operation can safely run with or without a transaction.
- Use **`NESTED`** when savepoint-based partial rollback is specifically required and supported.
- Use **`MANDATORY`** when a transaction is a strict prerequisite.
- Use **`NOT_SUPPORTED`** when an operation must execute outside the current transaction.
- Use **`NEVER`** when the presence of a transaction should be considered a programming error.

## One-Line Summary

```
REQUIRED       → Join existing or create new
REQUIRES_NEW   → Always create new
SUPPORTS       → Join if available
NOT_SUPPORTED  → Always execute without transaction
MANDATORY      → Existing transaction required
NEVER          → Existing transaction forbidden
NESTED         → Use nested scope/savepoint
```
# Limitations
## Qn\. Can we call transactional method inside non-transactional method in same class?
Yes — **you can call it**, but there is an important Spring caveat.

If the non-transactional method and `@Transactional` method are in the **same class**, the call is a **self-invocation**, so it bypasses Spring's transactional proxy.

```
@Service
public class OrderService {

    public void processOrder() {
        saveOrder(); // @Transactional is NOT applied here
    }

    @Transactional
    public void saveOrder() {
        // database operations
    }
}
```

In this case, `saveOrder()` **will execute**, but Spring will generally **not start a transaction** for it because the call doesn't pass through the Spring proxy.

### Recommended approach

Move the transactional method to another Spring bean:

```
@Service
public class OrderService {

    private final OrderTransactionService transactionService;

    public OrderService(OrderTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void processOrder() {
        transactionService.saveOrder();
    }
}
```

```
@Service
public class OrderTransactionService {

    @Transactional
    public void saveOrder() {
        // Runs inside a Spring-managed transaction
    }
}
```

Now the call goes through the Spring proxy:

```
OrderService
    |
    | processOrder()
    |
    v
OrderTransactionService proxy
    |
    | @Transactional
    v
saveOrder()
    |
    +--- Transaction started
```

### What if the caller itself is transactional?

Even if you have:

```
@Transactional
public void outerMethod() {
    innerMethod();
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void innerMethod() {
}
```

`innerMethod()` **will not get `REQUIRES_NEW` behavior** when called directly from `outerMethod()` in the same class. The self-invocation bypasses the proxy, so the existing transaction continues.

**Rule of thumb:**

> `@Transactional` works when the method is invoked **through the Spring proxy**, not when another method in the same class directly calls it.
---
# Spring `@Transactional` Isolation Levels
```sql
# To check isolation:
SHOW VARIABLES LIKE 'transaction_isolation';

# To set the isolation:
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;
```

## Overview

In Spring, the `isolation` attribute of `@Transactional` controls how a transaction is isolated from other concurrent transactions.

```
@Transactional(isolation = Isolation.READ_COMMITTED)
public void processOrder() {
    // business logic
}
```

Isolation is primarily a **database-level concurrency concept**. It determines what changes made by one transaction can be observed by other concurrent transactions.

Spring provides the following isolation levels:

```
public enum Isolation {
    DEFAULT,
    READ_UNCOMMITTED,
    READ_COMMITTED,
    REPEATABLE_READ,
    SERIALIZABLE
}
```

---

# Why Do We Need Transaction Isolation?

Consider two transactions executing at the same time:

```
Transaction A                 Transaction B
     |                              |
     |-- Read account = $100        |
     |                              |
     |                              |-- Update account = $50
     |                              |
     |-- Read account = $50         |
     |                              |
```

Transaction A saw different values during the same transaction.

Isolation levels define how much of this concurrent behavior is allowed.

---

# Transaction Concurrency Problems

There are three commonly discussed read anomalies:

1. Dirty Read
2. Non-Repeatable Read
3. Phantom Read

Understanding these is important before understanding the isolation levels.

---

# 1\. Dirty Read

A **dirty read** occurs when one transaction reads data that another transaction has modified but not yet committed.

Example:

```
Transaction A                    Transaction B
     |                                |
     |                                | UPDATE balance = 500
     |                                | (not committed)
     |                                |
     | SELECT balance                 |
     | ---> 500                       |
     |                                |
     |                                | ROLLBACK
     |                                |
```

Transaction A read `500`, but Transaction B eventually rolled back the change.

Therefore, the value read by Transaction A was never actually committed.

```
Dirty Read = Reading uncommitted data
```

---

# 2\. Non-Repeatable Read

A **non-repeatable read** occurs when a transaction reads the same row twice and gets different values because another transaction modified and committed that row between the reads.

Example:

```
Transaction A                    Transaction B
     |                                |
     | SELECT balance                 |
     | ---> 100                       |
     |                                |
     |                                | UPDATE balance = 200
     |                                | COMMIT
     |                                |
     | SELECT balance                 |
     | ---> 200                       |
     |                                |
```

Transaction A read the same row twice but received different values.

```
Non-Repeatable Read =
Same row + same transaction + different values
```

---

# 3\. Phantom Read

A **phantom read** occurs when a transaction executes the same query twice and the second execution returns additional or fewer rows because another transaction inserted, deleted, or modified rows matching the query condition.

Example:

```
Transaction A                    Transaction B
     |                                |
     | SELECT * FROM orders           |
     | WHERE amount > 100             |
     | ---> 5 rows                    |
     |                                |
     |                                | INSERT order
     |                                | amount = 200
     |                                | COMMIT
     |                                |
     | SELECT * FROM orders           |
     | WHERE amount > 100             |
     | ---> 6 rows                    |
     |                                |
```

The additional row is called a **phantom row**.

```
Phantom Read =
Same query + same transaction + different set of rows
```

---

# Spring Isolation Levels

## 1\. `DEFAULT`

```
@Transactional(isolation = Isolation.DEFAULT)
public void processOrder() {
}
```

`DEFAULT` means Spring uses the default isolation level configured by the underlying database.

Spring does **not** choose a universal isolation level for `DEFAULT`.

For example, the database may have its own default isolation configuration.

### Key point

> `DEFAULT` = **Use the database's default isolation level.**

This is the most common choice when you don't have a specific concurrency requirement.

---

# 2\. `READ_UNCOMMITTED`

```
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public void readData() {
}
```

This is the lowest isolation level.

A transaction may read changes made by another transaction even before those changes are committed.

### Allows

- Dirty reads
- Non-repeatable reads
- Phantom reads

Conceptually:

```
Transaction A                    Transaction B
     |                                |
     |                                | UPDATE value
     |                                | (not committed)
     |                                |
     | READ value                     |
     | ---> sees B's change           |
     |                                |
     |                                | ROLLBACK
```

Transaction A may have read data that never becomes permanent.

### Advantages

- High concurrency
- Potentially fewer locking restrictions
- Useful when approximate/stale data is acceptable

### Disadvantages

- Can read invalid/uncommitted data
- Results may be inconsistent

### Typical use cases

Rarely appropriate for critical business operations.

### Key point

> `READ_UNCOMMITTED` = **You may read uncommitted changes.**

---

# 3\. `READ_COMMITTED`

```
@Transactional(isolation = Isolation.READ_COMMITTED)
public void processOrder() {
}
```

A transaction can only read data that has been committed by other transactions.

Therefore, dirty reads are prevented.

### Prevents

- Dirty reads

### Does not necessarily prevent

- Non-repeatable reads
- Phantom reads

Example:

```
Transaction A                    Transaction B
     |                                |
     | SELECT balance                 |
     | ---> 100                       |
     |                                |
     |                                | UPDATE balance = 200
     |                                | COMMIT
     |                                |
     | SELECT balance                 |
     | ---> 200                       |
```

The first read sees `100`.

The second read sees `200`.

The data was committed, so there is no dirty read, but the result changed between reads.

### Advantages

- Good balance between consistency and concurrency
- Prevents dirty reads
- Commonly used isolation level

### Key point

> `READ_COMMITTED` = **Only read committed data.**

---

# 4\. `REPEATABLE_READ`

```
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void processOrder() {
}
```

`REPEATABLE_READ` ensures that once a transaction reads a row, repeated reads of that row generally return a consistent result within the same transaction.

### Prevents

- Dirty reads
- Non-repeatable reads

### Phantom reads

The SQL standard allows phantom reads at `REPEATABLE_READ`, although actual behavior depends on the database and its implementation.

For example, some databases provide stronger behavior than the minimum required by the SQL standard.

### Example

```
Transaction A                    Transaction B
     |                                |
     | SELECT balance                 |
     | ---> 100                       |
     |                                |
     |                                | UPDATE balance = 200
     |                                | COMMIT
     |                                |
     | SELECT balance                 |
     | ---> 100                       |
```

The transaction continues to see the same version/value of the row according to the database's concurrency model.

### Advantages

- Stronger consistency than `READ_COMMITTED`
- Prevents non-repeatable reads
- Useful when the same rows are read multiple times

### Disadvantages

- Can reduce concurrency
- May increase locking/versioning overhead
- Exact behavior differs between databases

### Key point

> `REPEATABLE_READ` = **Repeated reads of the same row remain consistent.**

---

# 5\. `SERIALIZABLE`

```
@Transactional(isolation = Isolation.SERIALIZABLE)
public void processOrder() {
}
```

`SERIALIZABLE` provides the strongest standard isolation level.

The goal is to make concurrent transactions behave as though they were executed one after another.

Conceptually:

```
Transaction A
     |
     |---- completes
     |
Transaction B
     |
     |---- completes
```

instead of allowing conflicting operations to freely overlap.

### Prevents

- Dirty reads
- Non-repeatable reads
- Phantom reads

### Advantages

- Highest consistency
- Strong protection against concurrency anomalies

### Disadvantages

- Lower concurrency
- More locking/contention or equivalent database coordination
- Greater possibility of blocking
- Potentially lower throughput

### Typical use cases

Use when correctness under concurrency is more important than maximum throughput.

Examples can include highly sensitive operations where concurrent modifications must be strictly controlled.

### Key point

> `SERIALIZABLE` = **Strongest standard isolation; concurrent transactions behave as if serialized.**

---

# Isolation Level Comparison

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read | Concurrency |
| --- | --- | --- | --- | --- |
| `READ_UNCOMMITTED` | Possible | Possible | Possible | Highest |
| `READ_COMMITTED` | Prevented | Possible | Possible | High |
| `REPEATABLE_READ` | Prevented | Prevented | Database-dependent | Medium |
| `SERIALIZABLE` | Prevented | Prevented | Prevented | Lowest |
| `DEFAULT` | Database-dependent | Database-dependent | Database-dependent | Database-dependent |

> The table describes the standard isolation semantics. Actual behavior can vary by database engine, storage engine, transaction manager, and concurrency implementation.

---

# Visual Comparison

```
Consistency
    ^
    |
    |                         SERIALIZABLE
    |                              |
    |                    REPEATABLE_READ
    |                              |
    |                      READ_COMMITTED
    |                              |
    |                    READ_UNCOMMITTED
    |
    +------------------------------------------> Concurrency
```

Generally:

```
Higher Isolation
       ↓
Lower Concurrency
       ↓
Potentially Higher Contention
```

The relationship is not an absolute rule because modern databases may implement isolation using different mechanisms such as locking or MVCC.

---

# `@Transactional` Example

A service method can specify both propagation and isolation:

```
@Transactional(
    propagation = Propagation.REQUIRED,
    isolation = Isolation.READ_COMMITTED
)
public void createOrder(Order order) {

    orderRepository.save(order);

    paymentService.processPayment(order);
}
```

Here:

```
Propagation:
    REQUIRED
    ↓
    Join existing transaction or create a new one

Isolation:
    READ_COMMITTED
    ↓
    Do not read uncommitted changes
```

Propagation and isolation solve **different problems**.

---

# Propagation vs Isolation

These two concepts are often confused.

## Propagation

Propagation answers:

> **What should happen to the transaction when this method is called?**

Examples:

```
REQUIRED
REQUIRES_NEW
NESTED
SUPPORTS
```

For example:

```
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

means:

```
Create a separate transaction.
```

---

## Isolation

Isolation answers:

> **How isolated should this transaction be from concurrent transactions?**

Examples:

```
READ_UNCOMMITTED
READ_COMMITTED
REPEATABLE_READ
SERIALIZABLE
```

For example:

```
@Transactional(isolation = Isolation.SERIALIZABLE)
```

means:

```
Use a very strong isolation level for concurrent operations.
```

---

# Propagation + Isolation Together

You can configure both:

```
@Transactional(
    propagation = Propagation.REQUIRES_NEW,
    isolation = Isolation.SERIALIZABLE
)
public void processPayment() {
}
```

This means:

```
Propagation
    ↓
Start/use a NEW transaction

Isolation
    ↓
Use SERIALIZABLE isolation for that transaction
```

---

# Important: Isolation Is Primarily Database-Dependent

Spring's `Isolation` enum provides a standard abstraction, but the actual transaction behavior is implemented by the database and transaction manager.

For example:

```
@Transactional(isolation = Isolation.REPEATABLE_READ)
```

does not mean that every database will implement `REPEATABLE_READ` in exactly the same way.

Different databases can use different mechanisms, including:

- Locks
- MVCC
- Row versions
- Snapshot-based reads
- Predicate/range locking

Therefore, when choosing an isolation level, always consider the database being used.

---

# Important: `DEFAULT` vs Explicit Isolation

Most applications should not automatically specify:

```
@Transactional(isolation = Isolation.SERIALIZABLE)
```

just because it provides stronger consistency.

A higher isolation level can introduce unnecessary contention and reduce application throughput.

Usually, start with:

```
@Transactional
```

which means:

```
@Transactional(isolation = Isolation.DEFAULT)
```

and therefore uses the database's configured default isolation.

Choose an explicit isolation level only when the application's concurrency requirements justify it.

---

# Example: Preventing Concurrent Updates

Suppose two users try to purchase the last available item.

Initial state:

```
stock = 1
```

Without appropriate concurrency control:

```
Transaction A                  Transaction B
     |                              |
     | Read stock = 1               |
     |                              | Read stock = 1
     |                              |
     | stock = 0                    |
     |                              | stock = 0
     |                              |
     | Commit                       | Commit
```

Both transactions may believe they successfully purchased the item.

Simply selecting an isolation level is not always enough to solve every business concurrency problem.

Depending on the database and application design, you may need:

- Appropriate isolation
- Optimistic locking
- Pessimistic locking
- Atomic SQL updates
- Database constraints

For example, optimistic locking can be implemented with a version column:

```
@Entity
public class Product {

    @Id
    private Long id;

    private int stock;

    @Version
    private Long version;
}
```

Spring/JPA can then detect conflicting updates.

---

# Example: `SERIALIZABLE`

For a highly sensitive operation:

```
@Transactional(isolation = Isolation.SERIALIZABLE)
public void transferMoney(
        Long sourceAccount,
        Long targetAccount,
        BigDecimal amount) {

    // Read balances
    // Validate balances
    // Update accounts
}
```

The stronger isolation level can prevent conflicting concurrent transactions from producing inconsistent results.

However, this does not automatically mean that `SERIALIZABLE` is the best solution. The database's locking/MVCC behavior and the transaction's duration must be considered.

---

# Isolation and Read/Write Operations

A useful way to think about isolation is:

```
Transaction
    |
    +-- SELECT
    |     |
    |     +-- What other transactions can I see?
    |
    +-- UPDATE
          |
          +-- How do concurrent modifications interact?
```

Isolation controls the visibility and concurrency behavior of transactions.

It does **not** simply mean:

```
"Lock everything."
```

Different databases implement isolation differently.

---

# Common Interview Questions

## What is the default isolation level in Spring?

Spring's default is:

```
Isolation.DEFAULT
```

This means Spring uses the underlying database's default isolation level.

---

## Which isolation level prevents dirty reads?

```
READ_COMMITTED
```

and stronger isolation levels prevent dirty reads.

---

## Which isolation level allows dirty reads?

```
READ_UNCOMMITTED
```

---

## Which isolation level prevents non-repeatable reads?

Typically:

```
REPEATABLE_READ
```

and:

```
SERIALIZABLE
```

---

## Which isolation level prevents phantom reads?

The standard guarantee is provided by:

```
SERIALIZABLE
```

`REPEATABLE_READ` behavior regarding phantoms can vary by database implementation.

---

## Does `REQUIRES_NEW` mean higher isolation?

No.

These are independent concepts.

```
@Transactional(
    propagation = Propagation.REQUIRES_NEW,
    isolation = Isolation.READ_COMMITTED
)
```

means:

```
REQUIRES_NEW
    → Create a new transaction

READ_COMMITTED
    → Use READ_COMMITTED isolation for that transaction
```

---

# Recommended Selection Guide

### Use `DEFAULT`

When:

- You don't have a specific isolation requirement.
- The database's default is appropriate.
- You want the simplest configuration.

```
@Transactional
```

---

### Use `READ_COMMITTED`

When:

- Dirty reads must be prevented.
- You want a common balance between consistency and concurrency.
- Your database/application requirements specifically call for it.

```
@Transactional(isolation = Isolation.READ_COMMITTED)
```

---

### Use `REPEATABLE_READ`

When:

- The same rows may be read multiple times within a transaction.
- You need stronger consistency than `READ_COMMITTED`.
- The database's implementation is understood and suitable.

```
@Transactional(isolation = Isolation.REPEATABLE_READ)
```

---

### Use `SERIALIZABLE`

When:

- Strong consistency is essential.
- Conflicting concurrent operations must be heavily restricted.
- The performance/locking implications are acceptable.

```
@Transactional(isolation = Isolation.SERIALIZABLE)
```

Avoid using it indiscriminately because it can significantly affect concurrency.

---

# Important Spring Caveat: Self-Invocation

As with other `@Transactional` settings, isolation is applied through Spring's transaction proxy.

For example:

```
@Service
public class OrderService {

    public void process() {
        saveOrder();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void saveOrder() {
        // ...
    }
}
```

The direct call:

```
saveOrder();
```

is a self-invocation.

It bypasses the Spring proxy, so the `@Transactional` configuration on `saveOrder()` may not be applied.

A common solution is to put the transactional method in another Spring-managed service:

```
@Service
public class OrderService {

    private final TransactionService transactionService;

    public OrderService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void process() {
        transactionService.saveOrder();
    }
}
```

```
@Service
public class TransactionService {

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void saveOrder() {
        // Transaction + isolation applied here
    }
}
```

---

# Final Cheat Sheet

```
Isolation.DEFAULT
    ↓
Use database default

READ_UNCOMMITTED
    ↓
Dirty reads possible
    ↓
Highest concurrency / weakest isolation

READ_COMMITTED
    ↓
Dirty reads prevented
    ↓
Non-repeatable reads possible

REPEATABLE_READ
    ↓
Dirty reads prevented
    ↓
Non-repeatable reads prevented
    ↓
Phantom behavior depends on database implementation

SERIALIZABLE
    ↓
Strongest standard isolation
    ↓
Dirty + non-repeatable + phantom reads prevented
    ↓
Lowest concurrency / potentially highest contention
```

## Quick Comparison

| Isolation | Dirty Read | Non-Repeatable Read | Phantom Read | Typical Choice |
| --- | --- | --- | --- | --- |
| `DEFAULT` | DB-dependent | DB-dependent | DB-dependent | **Default choice** |
| `READ_UNCOMMITTED` | ❌ Prevented? **No** | ❌ | ❌ | Rare |
| `READ_COMMITTED` | ✅ | ❌ | ❌ | Common |
| `REPEATABLE_READ` | ✅ | ✅ | DB-dependent | When repeatable reads matter |
| `SERIALIZABLE` | ✅ | ✅ | ✅ | Strong consistency |

> **Practical rule:** Start with `@Transactional` / `Isolation.DEFAULT`. Choose an explicit isolation level only when you have a demonstrated concurrency requirement, and validate its behavior and performance against your specific database.
---
## Qn\. I have added Isolation level as below in my application
```java
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public void readData() {
}
```
## Do I still need to change DB isolation level, to see the change?

No — **normally you do not need to change the database's global/default isolation level**.

If you have:

```
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public void readData() {
    // ...
}
```

Spring will request `READ_UNCOMMITTED` **for the transaction started for that method**, assuming the method is actually invoked through Spring's transactional proxy.

### Think of it this way

```
Database default isolation
        │
        │  can remain unchanged
        ▼
┌─────────────────────────────┐
│ Your application             │
│                             │
│ @Transactional(             │
│   isolation = READ_UNCOMMITTED│
│ )                           │
│                             │
│   ↓                         │
│ This transaction uses       │
│ READ_UNCOMMITTED            │
└─────────────────────────────┘
```

So, for example, if your database default is:

```
READ_COMMITTED
```

and your method specifies:

```
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
```

the transaction created for that method can use `READ_UNCOMMITTED` without changing the database-wide default.

## But there are 3 important conditions

### 1\. The method must actually run inside a Spring transaction

This works:

```
@Service
public class MyService {

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void readData() {
        // ...
    }
}
```

when called from another Spring bean:

```
myService.readData();
```

But be careful with self-invocation:

```
public void methodA() {
    readData();  // @Transactional may NOT be applied
}

@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public void readData() {
}
```

Here, `readData()` is called directly from the same class, so the Spring proxy is bypassed.

### 2\. The database must support the requested isolation level

Spring doesn't implement `READ_UNCOMMITTED` itself. It asks the underlying transaction/database infrastructure to use it.

Different databases have different isolation semantics and restrictions.

### 3\. Your test must involve concurrent transactions

Simply doing:

```
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public void readData() {
    repository.findById(1L);
}
```

doesn't visibly demonstrate anything.

To observe the difference, you need something like:

```
Transaction A                    Transaction B
     |                                |
     | UPDATE balance = 500           |
     |                                |
     |       NOT COMMITTED            |
     |                                |
     |                                | READ balance
     |                                | ---> 500
     |                                |
     | ROLLBACK                       |
     |                                |
```

With `READ_UNCOMMITTED`, Transaction B **may be able to see Transaction A's uncommitted change**.

With `READ_COMMITTED`, Transaction B should not see that uncommitted value.

## One important distinction

You **don't need to change the DB's default isolation level**, but you may need to verify what Spring/JDBC actually configured for the connection.

For example, you can check:

```
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public void readData() {
    // execute your query
}
```

and inspect the connection's isolation level during the transaction.
