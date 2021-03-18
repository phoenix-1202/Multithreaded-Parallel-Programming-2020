import kotlinx.atomicfu.*

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val usingFlag = atomic(0)
    private val core = atomic(Core<E>(INITIAL_CAPACITY, INITIAL_SIZE))

    override fun get(index: Int): E {
        if (index < core.value.size.value)
            return core.value.getArray()[index].value!!.value
        else
            throw IllegalArgumentException()
    }

    override fun put(index: Int, element: E) {
        while (true) {
            val curCore = core.value
            if (index < curCore.size.value) {
                val curNode = curCore.getArray()[index].value
                val newNode = Node(element)
                if (!curNode!!.isRemoved && curCore.getArray()[index].compareAndSet(curNode, newNode))
                    return
            } else
                throw IllegalArgumentException()
        }
    }

    override fun pushBack(element: E) {
        while (true) {
            val curCore = core.value
            val pos = curCore.size.value
            val newNode = Node(element)
            if (pos >= curCore.capacity) {
                if (!usingFlag.compareAndSet(expect = 0, update = 1))
                    continue
                val newCore = Core<E>(curCore.capacity * 2, pos)
                var i = 0
                while (i != pos) {
                    val curNode = curCore.getArray()[i].value
                    val newRemovedNode = RemovedNode(curNode!!.value)
                    if (!curCore.getArray()[i].compareAndSet(curNode, newRemovedNode))
                        continue
                    newCore.getArray()[i++].value = curNode
                }
                core.compareAndSet(curCore, newCore)
                usingFlag.value = 0
            } else if (curCore.getArray()[pos].compareAndSet(null, newNode) && curCore.size.compareAndSet(pos, pos + 1))
                return
        }
    }

    override val size: Int get() {
        return core.value.size.value
    }
}

open class Node<E> (v: E) {
    open val isRemoved = false
    val value = v
}

class RemovedNode<E>(v: E) : Node<E>(v) {
    override val isRemoved = true
}

private class Core<E> constructor(c: Int, s: Int) {
    private val array = atomicArrayOfNulls<Node<E>>(c)
    val size = atomic(s)
    val capacity = c

    fun getArray(): AtomicArray<Node<E>?> {
        return array
    }
}

private const val INITIAL_CAPACITY = 1
private const val INITIAL_SIZE = 0
