package nl.han.ica.datastructures;

import java.util.LinkedList;
import java.util.List;

public class HANStack<T> implements IHANStack<T> {
    // Maak hierbij gebruik van een List

    private final List<T> list = new LinkedList<>();

    @Override
    public void push(T value) {
        list.add(list.size(), value);
    }

    @Override
    public T pop() {
        T value = list.get(list.size() - 1);
        list.remove(list.size() - 1);
        return value;
    }

    @Override
    public T peek() {
        return list.get(list.size() - 1);
    }
}
