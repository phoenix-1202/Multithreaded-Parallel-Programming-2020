package stack;

import kotlinx.atomicfu.AtomicRef;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class StackImpl implements Stack {
    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node() {
            this.next = new AtomicRef<>(null);
            this.x = Integer.MIN_VALUE;
        }

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    private static final int SIZE = 50;
    private static final int WINDOW = 10;
    private static final int WAIT = 5;

    // head pointer
    private AtomicRef<Node> head = new AtomicRef<>(null);
    private Node emptyElement = new Node();
    private Random rand = new Random();

    private List<AtomicRef<Node>> eliminationArray = Collections.nCopies(SIZE, new AtomicRef<>(emptyElement));

    @Override
    public void push(int x)
    {
        Node newNode = new Node(x, null);
        int randPos = rand.nextInt(eliminationArray.size() - 2 * WINDOW) + WINDOW;
        for (int i = randPos - WINDOW; i < randPos + WINDOW; i++) {
            if (eliminationArray.get(i).compareAndSet(emptyElement, newNode)) {
                for (int wait = 0; wait < WAIT; wait++) { }
                if (!eliminationArray.get(i).compareAndSet(newNode, emptyElement))
                    return;
                break;
            }
        }
        while (true) {
            Node oldHead = head.getValue();
            Node newHead = new Node(x, oldHead);
            if (head.compareAndSet(oldHead, newHead))
                return;
        }
        //head.setValue(new Node(x, head.getValue()));
    }

    @Override
    public int pop() {
        int randPos = rand.nextInt(eliminationArray.size() - 2 * WINDOW) + WINDOW;
        for (int i = randPos - WINDOW; i < randPos + WINDOW; i++) {
            if (eliminationArray.get(i).compareAndSet(emptyElement, emptyElement))
                continue;
            Node top = eliminationArray.get(i).getValue();
            if (top != emptyElement && eliminationArray.get(i).compareAndSet(top, emptyElement))
                return top.x;
        }
        while (true) {
            Node oldHead = head.getValue();
            if (oldHead != null && head.compareAndSet(oldHead, oldHead.next.getValue()))
                return oldHead.x;
            else if (oldHead == null)
                return Integer.MIN_VALUE;
        }
        /*Node curHead = head.getValue();
        if (curHead == null) return Integer.MIN_VALUE;
        head.setValue(curHead.next.getValue());
        return curHead.x;*/
    }
}
