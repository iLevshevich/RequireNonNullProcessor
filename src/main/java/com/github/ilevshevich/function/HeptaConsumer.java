package com.github.ilevshevich.function;

import java.util.Objects;

@FunctionalInterface
public interface HeptaConsumer<T1, T2, T3, T4, T5, T6, T7> {
    void accept(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7);

    default HeptaConsumer<T1, T2, T3, T4, T5, T6, T7> andThen(final HeptaConsumer<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7> after) {
        Objects.requireNonNull(after);

        return (t1, t2, t3, t4, t5, t6, t7) -> {
            accept(t1, t2, t3, t4, t5, t6, t7);
            after.accept(t1, t2, t3, t4, t5, t6, t7);
        };
    }
}
