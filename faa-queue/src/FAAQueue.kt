import kotlinx.atomicfu.*
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.AtomicRef

@Suppress("UNCHECKED_CAST")
class FAAQueue<T> {
    private val head: AtomicRef<Segment>
    private val tail: AtomicRef<Segment>

    init {
        val firstNode = Segment()
        head = atomic(firstNode)
        tail = atomic(firstNode)
    }

    fun enqueue(x: T) {
        while (true) {
            val curTail = tail.value
            val enqIdx = curTail.enqIdx.getAndIncrement()
            if (enqIdx >= SEGMENT_SIZE) {
                val newTail = Segment(x)
                val flag = curTail.next.compareAndSet(null, newTail)
                tail.compareAndSet(curTail, curTail.next.value!!)
                if (flag) return
            } else {
                if (curTail.elements[enqIdx].compareAndSet(null, x)) {
                    return
                }
            }
        }
    }


    fun dequeue(): T? {
        while (true) {
            val curHead = head.value
            val deqIdx = curHead.deqIdx.getAndIncrement()
            if (deqIdx >= SEGMENT_SIZE) {
                val next = curHead.next
                if (next.compareAndSet(null, null)) return null
                next.value?.let { head.compareAndSet(curHead, it) }
            }
            else {
                val res = curHead.elements[deqIdx].getAndSet(DONE) ?: continue
                return res as T?
            }
        }
    }


    val isEmpty: Boolean get() {
        while (true) {
            val curHead = head.value
            if (curHead.isEmpty) {
                if (curHead.next.compareAndSet(null, null)) return true
                head.compareAndSet(curHead, curHead.next.value!!)
                continue
            } else {
                return false
            }
        }
    }
}

private class Segment {
    val next: AtomicRef<Segment?> = atomic(null)
    val enqIdx: AtomicInt = atomic(0)
    val deqIdx: AtomicInt = atomic(0)
    val elements = atomicArrayOfNulls<Any>(SEGMENT_SIZE)

    constructor()

    constructor(x: Any?) {
        enqIdx.getAndSet(1)
        elements[0].getAndSet(x)
    }

    val isEmpty: Boolean get() = deqIdx.value >= enqIdx.value || deqIdx.value >= SEGMENT_SIZE

}

private val DONE = Any()
const val SEGMENT_SIZE = 2