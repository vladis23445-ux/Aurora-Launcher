package dev.aurora.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Минималистичная синхронная шина событий.
 * Ключ — класс события, слушатель — Consumer. Ошибки слушателя не ломают игру.
 */
public final class EventBus {

    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    public <T> void subscribe(Class<T> type, Consumer<T> listener) {
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <T> T post(T event) {
        List<Consumer<?>> list = listeners.get(event.getClass());
        if (list != null) {
            for (Consumer<?> raw : list) {
                try {
                    ((Consumer<T>) raw).accept(event);
                } catch (Throwable ignored) {
                }
            }
        }
        return event;
    }
}
