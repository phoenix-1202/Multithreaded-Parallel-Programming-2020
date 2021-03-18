package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {

    private class Node {
        boolean isRemoved;
    }

    public class NodeActive extends Node {
        AtomicRef<Node> next;
        int x;

        NodeActive(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
            this.isRemoved = false;
        }
    }

    public class NodeRemoved extends Node {
        NodeActive next;

        NodeRemoved(NodeActive n) {
            this.next = n;
            this.isRemoved = true;
        }
    }

    private class Window {
        NodeActive cur, next;

        Window(NodeActive c, NodeActive n) {
            this.cur = c;
            this.next = n;
        }
    }

    private final AtomicRef<NodeActive> head = new AtomicRef<>(new NodeActive(Integer.MIN_VALUE, new NodeActive(Integer.MAX_VALUE, null)));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        while (true) {
            NodeActive cur = head.getValue();
            NodeActive next = (NodeActive) cur.next.getValue();
            boolean flag = false;
            while (next.x < x) {
                Node node = next.next.getValue();
                if (!node.isRemoved) {
                    cur = next;
                    next = (NodeActive) node;
                }
                else {
                    NodeActive newNext = ((NodeRemoved) node).next;
                    if (cur.next.compareAndSet(next, newNext))
                        next = newNext;
                    else {
                        flag = true;
                        break;
                    }
                }
            }
            if (flag)
                continue;
            return new Window(cur, next);
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window window = findWindow(x);
            if (contains(x))
                return false;
            Node newNode = new NodeActive(x, window.next);
            if (window.cur.next.compareAndSet(window.next, newNode))
                return true;
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window window = findWindow(x);
            if (window.next.x == x) {
                Node node = window.next.next.getValue();
                if (node.isRemoved)
                    return false;
                NodeRemoved newNode = new NodeRemoved((NodeActive) node);
                if (!window.next.next.compareAndSet(node, newNode))
                    continue;
                window.cur.next.compareAndSet(window.next, node);
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean contains(int x) {
        Window window = findWindow(x);
        Node node = window.next.next.getValue();
        return window.next.x == x && !node.isRemoved;
    }
}