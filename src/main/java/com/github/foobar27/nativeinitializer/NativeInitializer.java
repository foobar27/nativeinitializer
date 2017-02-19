package com.github.foobar27.nativeinitializer;

import java.io.IOException;
import java.util.function.Supplier;

public final class NativeInitializer<T> implements Supplier<T> {
    private NativeLoader loader; // does not need to be volatile
    private final Supplier<T> initializer;
    private final Object monitor = new Object();


    private volatile boolean initialized;
    private T value; // does not need to be volatile

    public NativeInitializer(NativeLoader loader, Supplier<T> initializer) {
        this.initializer = initializer;
        this.loader = loader;
    }

    public void setNativeLoader(NativeLoader loader) throws NativeLoaderAlreadyInitializedException {
        if (initialized) {
            throw new NativeLoaderAlreadyInitializedException();
        }
        this.loader = loader;
    }

    @Override
    public T get() {
        if (!initialized) {
            synchronized (monitor) {
                // double-checked locking
                if (!initialized) {
                    try {
                        loader.load();
                    } catch (IOException e) {
                        throw new NativeLoaderException(e);
                    }
                    T t = initializer.get();
                    value = t;
                    initialized = true;
                    return t;
                }
            }
        }
        // We will read a non-volatile value here.
        // However just before we already read 'initialize', which is also volatile.
        // Thus we don't need to make 'value' volatile too.
        return value;
    }

}