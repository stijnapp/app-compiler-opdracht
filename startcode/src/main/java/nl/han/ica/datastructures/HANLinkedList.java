package nl.han.ica.datastructures;

import org.antlr.v4.runtime.misc.NotNull;

import java.util.Iterator;

public class HANLinkedList<T> implements IHANLinkedList<T> {

    private class Node {
        T value;
        Node next;
        Node previous;

        Node(T value) {
            this.value = value;
            this.next = null;
            this.previous = null;
        }
    }

    private Node header;
    private Node tail;
    private int size;

    public HANLinkedList() {
        this.clear();
    }

    @Override
    public void addFirst(T value) {
        this.insert(0, value);
    }

    @Override
    public void clear() {
        this.header = new Node(null);
        this.tail = new Node(null);
        this.header.next = this.tail;
        this.tail.previous = this.header;
        this.size = 0;
    }

    private Node getNode(int index) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException();

        Node current;
        if (index < size / 2) {
            current = header.next;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
        } else {
            current = tail;
            for (int i = size; i > index; i--) {
                current = current.previous;
            }
        }
        return current;
    }

    @Override
    public void insert(int index, T value) {
        Node target = getNode(index);
        Node newNode = new Node(value);

        newNode.next = target;
        newNode.previous = target.previous;
        target.previous.next = newNode;
        target.previous = newNode;

        size++;
    }

    @Override
    public void delete(int pos) {
        Node target = getNode(pos);

        target.previous.next = target.next;
        target.next.previous = target.previous;

        size--;
    }

    @Override
    public T get(int pos) {
        return getNode(pos).value;
    }

    @Override
    public void removeFirst() {
        this.delete(0);
    }

    @Override
    public T getFirst() {
        return get(0);
    }

    @Override
    public int getSize() {
        return this.size;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node current = header.next;

            @Override
            public boolean hasNext() {
                return current != tail;
            }

            @Override
            public T next() {
                T value = current.value;
                current = current.next;
                return value;
            }
        };
    }
}

