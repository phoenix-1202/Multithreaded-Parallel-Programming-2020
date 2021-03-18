import java.util.concurrent.locks.ReentrantLock

/**
 * Bank implementation.
 * This implementation is thread-safe.
 *
 * @author Chebotareva Olesya (@phoenix-1202)
 */
class BankImpl(n: Int) : Bank {
    private val accounts: Array<Account?>
    override val numberOfAccounts: Int
        get() = accounts.size

    override fun getAmount(index: Int): Long {
        accounts[index]!!.lock.lock()
        return try {
            accounts[index]!!.amount
        } finally {
            accounts[index]!!.lock.unlock()
        }
    }

    override val totalAmount: Long
        get() {
            var sum: Long = 0
            try {
                for (account in accounts) {
                    account!!.lock.lock()
                    sum += account.amount
                }
            } finally {
                for (account in accounts) {
                    account!!.lock.unlock()
                }
            }
            return sum
        }

    override fun deposit(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        return try {
            account!!.lock.lock()
            check(!(amount > Bank.MAX_AMOUNT || account.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
            account.amount += amount
            account.amount
        } finally {
            account!!.lock.unlock()
        }
    }

    override fun withdraw(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        return try {
            account!!.lock.lock()
            check(account.amount - amount >= 0) { "Underflow" }
            account.amount -= amount
            account.amount
        } finally {
            account!!.lock.unlock()
        }
    }

    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }
        val from = accounts[fromIndex]
        val to = accounts[toIndex]
        try {
            if (fromIndex > toIndex) {
                to!!.lock.lock()
                from!!.lock.lock()
            } else {
                from!!.lock.lock()
                to!!.lock.lock()
            }
            check(amount <= from.amount) { "Underflow" }
            check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
            from.amount -= amount
            to.amount += amount
        } finally {
            from!!.lock.unlock()
            to!!.lock.unlock()
        }
    }

    internal class Account {
        var amount: Long = 0
        var lock: ReentrantLock

        init {
            lock = ReentrantLock(true)
        }
    }

    init {
        accounts = arrayOfNulls(n)
        for (i in 0 until n) {
            accounts[i] = Account()
        }
    }
}