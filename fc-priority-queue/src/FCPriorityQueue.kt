import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import java.lang.Integer.min
import java.util.*

class FCPriorityQueue<E: Comparable<E>> {
    private val q = PriorityQueue<E>()
    private val operationsArray = atomicArrayOfNulls<Node<E>>(SIZE)
    private val random = Random()
    private val locked: AtomicBoolean = atomic(false)

    fun add(element: E) {
        if (doAdd(element))
            return

        val node = Node(Operation.ADD, element)
        var index: Int
        var flag = true

        do {
            index = random.nextInt(SIZE)
            for (i in index until min(index + DELTA, SIZE)) {
                val newFlag = operationsArray[i].compareAndSet(null, node)
                if (!newFlag && doAdd(element)) return
                else if (newFlag) {
                    flag = false
                    index = i
                    break
                }
            }
        } while (flag)

        while (true) {
            if (!locked.value) {
                if (locked.compareAndSet(expect = false, update = true)) {
                    val result = operationsArray[index]
                    if (result.value!!.op == Operation.ADD)
                        q.add(element)
                    operationsArray[index].compareAndSet(result.value, null)
                    doAllOperations()
                    locked.compareAndSet(expect = true, update = false)
                    break
                }
            }
            if (operationsArray[index].compareAndSet(Node(Operation.FINISH, null), null))
                break
        }
    }

    fun poll(): E? {
        var ans = doPoll()
        if (ans.hasResult == 1)
            return ans.element

        val node = Node<E>(Operation.POLL, null)
        var index: Int
        var flag = true

        do {
            index = random.nextInt(SIZE)
            for (i in index until min(index + DELTA, SIZE)) {
                val newFlag = operationsArray[i].compareAndSet(null, node)
                if (!newFlag) {
                    ans = doPoll()
                    if (ans.hasResult == 1) return ans.element
                }
                else if (newFlag) {
                    flag = false
                    index = i
                    break
                }
            }
        } while (flag)

        while (true) {
            if (!locked.value) {
                if (locked.compareAndSet(expect = false, update = true)) {
                    val result = operationsArray[index]
                    var answer: E? = result.value!!.element
                    if (result.value!!.op != Operation.FINISH)
                        answer = q.poll()
                    operationsArray[index].compareAndSet(result.value, null)
                    doAllOperations()
                    locked.compareAndSet(expect = true, update = false)
                    return answer
                }
            }
            val answer = operationsArray[index].value!!.element
            if (operationsArray[index].compareAndSet(Node(Operation.FINISH, null), null))
                return answer
        }
    }

    fun peek(): E? {
        return q.peek()
    }

    private fun doAdd(element: E): Boolean {
        if (locked.value)
            return false
        if (locked.compareAndSet(expect = false, update = true)) {
            q.add(element)
            doAllOperations()
            locked.compareAndSet(expect = true, update = false)
            return true
        }
        return false
    }

    private fun doPoll(): PollResult<E?> {
        if (locked.value)
            return PollResult(0)
        if (locked.compareAndSet(expect = false, update = true)) {
            val res = q.poll()
            doAllOperations()
            locked.compareAndSet(expect = true, update = false)
            return PollResult(1, res)
        }
        return PollResult(0)
    }

    private fun doAllOperations() {
        for (i in 0 until SIZE) {
            val curOp = operationsArray[i].value
            if (curOp != null) {
                if (curOp.op == Operation.ADD) {
                    q.add(curOp.element)
                    operationsArray[i].value = Node(Operation.FINISH, null)
                }
                else if (curOp.op == Operation.POLL)
                    operationsArray[i].value = Node(Operation.FINISH, q.poll())
            }
        }
    }

    private class Node<E>(o: Operation, el: E?) {
        val op: Operation = o
        val element: E? = el
    }

    private class PollResult<E>(p: Int, el: E? = null) {
        val hasResult: Int = p
        val element: E? = el
    }

    enum class Operation {
        ADD,
        POLL,
        FINISH
    }
}

const val SIZE = 20
const val DELTA = 5