package nl.han.ica.datastructures;

import java.util.LinkedList;
import java.util.List;

public class HANQueue<T> implements IHANQueue<T> {
    private final List<T> list = new LinkedList<>();

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public void enqueue(T value) {
        list.add(list.size() - 1, value);
    }

    @Override
    public T dequeue() {
        T value = list.get(0);
        list.remove(0);
        return value;
    }

    @Override
    public T peek() {
        return list.get(0);
    }

    @Override
    public int getSize() {
        return list.size();
    }
}
