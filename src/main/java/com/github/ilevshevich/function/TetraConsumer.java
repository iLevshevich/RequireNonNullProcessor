package com.github.ilevshevich.function;

import java.util.Objects;

@FunctionalInterface
public interface TetraConsumer<T1, T2, T3, T4> {
    void accept(T1 t1, T2 t2, T3 t3, T4 t4);

    default TetraConsumer<T1, T2, T3, T4> andThen(final TetraConsumer<? super T1, ? super T2, ? super T3, ? super T4> after) {
        Objects.requireNonNull(after);

        return (t1, t2, t3, t4) -> {
            accept(t1, t2, t3, t4);
            after.accept(t1, t2, t3, t4);
        };
    }
}
