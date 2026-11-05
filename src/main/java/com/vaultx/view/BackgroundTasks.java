package com.vaultx.view;

import javafx.concurrent.Task;
import javafx.application.Platform;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class BackgroundTasks implements AutoCloseable {
    private final ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "vaultx-worker");
        thread.setDaemon(true);
        return thread;
    });

    public <T> void submit(Supplier<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        Task<T> task = new Task<>() {
            @Override protected T call() { return work.get(); }
        };
        task.setOnSucceeded(event -> success.accept(task.getValue()));
        task.setOnFailed(event -> failure.accept(task.getException()));
        executor.submit(task);
    }

    public void submit(Runnable work, Runnable success, Consumer<Throwable> failure) {
        submit(() -> { work.run(); return null; }, ignored -> success.run(), failure);
    }

    public static void later(Runnable action) {
        if (Platform.isFxApplicationThread()) action.run(); else Platform.runLater(action);
    }

    @Override public void close() { executor.shutdownNow(); }
}
