package com.github.ilevshevich.util;

public final class Cast {
    public static <T, R> R cast(final T value, Class<R> clazz) {
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }

        throw new ClassCastException(String.format("Cannot cast '%s' to '%s'", value.getClass().getName(), clazz.getName()));
    }
}
