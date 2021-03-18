/**
 * @author Chebotareva Olesya (@phoenix-1202)
 */
public class Solution implements AtomicCounter {
    public final Node head = new Node(0);
    public final ThreadLocal<Node> tail = ThreadLocal.withInitial(() -> head);

    public int getAndAdd(int x) {
        int ans = 0;
        Node newNode;
        do {
            Node cur = tail.get();
            newNode = new Node(x + cur.x);
            tail.set(tail.get().next.decide(newNode));
            ans = cur.x;
        } while (!newNode.equals(tail.get()));
        return ans;
    }

    private static class Node {
        public final int x;
        public final Consensus<Node> next;

        Node(int xx) {
            this.x = xx;
            this.next = new Consensus<>();
        }
    }
}