package io.github.wokii;
import java.util.*;

public class LinkedListNode {

    public LinkedListNode(List<Integer> val) {
        this.val = val;
        this.prev = null;
        this.next = null;


    }

    List<Integer> val;
    LinkedListNode prev;
    LinkedListNode next;
    int length;

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return this.length;
    }

    public void addLength(int toAdd) {
        this.length += toAdd;
    }

    static void connect(LinkedListNode first, LinkedListNode second) {
        first.next = second;
        second.prev = first;

    }
    static void remove(LinkedListNode node) {
        if (node.prev != null) {
            node.prev.next = node.next;
            node.prev = null;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
            node.next = null;
        }

    }

    public String toStringAll() {
        String nextStr = "";
        if (next != null) {
            nextStr += next.toStringAll();
        }

        return "([" + super.toString()+"], with val = " + val.toString() + ", next = " + nextStr + ")";
    }
}
