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