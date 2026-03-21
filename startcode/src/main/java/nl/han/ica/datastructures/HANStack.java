package nl.han.ica.datastructures;

import java.util.LinkedList;
import java.util.List;

public class HANStack<T> implements IHANStack<T> {
    // Maak hierbij gebruik van een List

    private final List<T> list = new LinkedList<>();

    @Override
    public void push(T value) {
        list.add(lastIndex(), value);
    }

    @Override
    public T pop() {
        T value = list.get(lastIndex());
        list.remove(lastIndex());
        return value;
    }

    @Override
    public T peek() {
        return list.get(lastIndex());
    }

    private int lastIndex() {
        return list.size() - 1;
    }
}
