package fr.supermax_8.spawndecoration.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class EntityIdProvider {

    private final static AtomicInteger entityId = new AtomicInteger(1500000);

    public static int provide() {
        return entityId.incrementAndGet();
    }

}