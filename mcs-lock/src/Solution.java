import java.util.concurrent.atomic.AtomicReference;

public class Solution implements Lock<Solution.Node> {

    final Environment env;
    final AtomicReference<Node> tail;

    public static class Node {
        final AtomicReference<Boolean> locked = new AtomicReference<>(false);
        final AtomicReference<Node> next = new AtomicReference<>(null);
        final Thread thread = Thread.currentThread();
    }

    public Solution(Environment environment) {
        this.env = environment;
        this.tail = new AtomicReference<>(null);
    }

    @Override
    public Node lock() {
        Node my = new Node();
        my.locked.set(true);
        Node node = tail.getAndSet(my);
        if (node == null)
            return my;
        node.next.set(my);
        while (my.locked.get())
            env.park();
        return my;
    }

    @Override
    public void unlock(Node node) {
        if (node.next.get() == null) {
            if (tail.compareAndSet(node, null))
                return;
            while (node.next.get() == null)
                assert true;
        }
        node.next.get().locked.set(false);
        env.unpark(node.next.get().thread);
    }
}
