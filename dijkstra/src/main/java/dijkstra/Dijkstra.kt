package dijkstra

import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.*
import kotlin.Comparator
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }


fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    start.distance = 0
    val q = MultiQueue(workers * 2, NODE_DISTANCE_COMPARATOR)
    q.add(start)
    val onFinish = Phaser(workers + 1)
    var qSize = 1

    repeat(workers) {
        thread {
            while (true) {
                val cur = synchronized(q) { q.poll() }
                if (cur != null) {
                    for (edge in cur.outgoingEdges) {
                        var flag = true
                        while (flag) {
                            val dist = edge.to.distance
                            val newDist = edge.weight + cur.distance
                            if (dist > newDist) {
                                flag = !edge.to.casDistance(dist, newDist)
                                if (!flag) {
                                    q.add(edge.to)
                                    qSize++
                                }
                            } else
                                flag = false
                        }
                    }
                    qSize--
                } else if (--qSize <= 0)
                    break
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}


class MultiQueue(workers: Int, nodeDistanceComparator: Comparator<Node>) {
    private val list = arrayListOf<PriorityQueue<Node>>()
    private val capacity = AtomicInteger(workers)
    private val rand = Random()

    init {
        for (i in 0 until capacity.get()) {
            list.add(PriorityQueue(nodeDistanceComparator));
        }
    }

    fun add(node: Node) {
        val index = rand.nextInt(capacity.get())
        synchronized(list[index]) { list[index].add(node) }
    }

    fun poll(): Node? {
        val index1 = rand.nextInt(capacity.get() - 1)
        var index2 = rand.nextInt(capacity.get())
        while (index1 >= index2)
            index2 = rand.nextInt(capacity.get())
        synchronized(list[index1]) {
            val min1 = list[index1].peek()
            synchronized(list[index2]) {
                val min2 = list[index2].peek()
                if (min1 != null && min2 != null)
                    return if (min1.distance < min2.distance) list[index1].poll() else list[index2].poll()
                else if (min1 == null && min2 != null)
                    return list[index2].poll()
                else if (min2 == null && min1 != null)
                    return list[index1].poll()
            }
        }
        var i = -1
        while (true) {
            if (++i >= capacity.get())
                return null
            synchronized(list[i]) {
                if (list[i].size > 0)
                    return list[i].poll()
            }
        }
    }
}