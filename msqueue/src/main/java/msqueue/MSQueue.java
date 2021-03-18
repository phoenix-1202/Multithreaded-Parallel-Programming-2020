package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private AtomicRef<Node> head;
    private AtomicRef<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0, null);
        this.head = new AtomicRef<>(dummy);
        this.tail = new AtomicRef<>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x, null);
        while (true) {
            Node curTail = tail.getValue();
            if (!curTail.next.compareAndSet(null, newTail))
                tail.compareAndSet(curTail, curTail.next.getValue());
            else {
                tail.compareAndSet(curTail, newTail);
                break;
            }
        }
    }

    @Override
    public int dequeue() {
        while (true) {
            Node curHead = head.getValue();
            AtomicRef<Node> next = curHead.next;
            if (!next.compareAndSet(null, next.getValue()) && head.compareAndSet(curHead, next.getValue()))
                return next.getValue().x;
            if (next.compareAndSet(null, null))
                return Integer.MIN_VALUE;
        }
    }

    @Override
    public int peek() {
        AtomicRef<Node> curHead = head;
        AtomicRef<Node> next = curHead.getValue().next;
        if (!next.compareAndSet(null, next.getValue()))
            return next.getValue().x;
        else
            return Integer.MIN_VALUE;
    }

    private class Node {
        final int x;
        AtomicRef<Node> next;

        Node(int x, Node n) {
            this.next = new AtomicRef<>(n);
            this.x = x;
        }
    }
}