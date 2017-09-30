package com.icthh.xm.ms.entity.domain.ext;

import java.util.Objects;

/**
 * The {@link IdOrKey} class.
 */
public final class IdOrKey {

    private final String value;
    private Long id;
    private String key;
    private Boolean isKey;

    private IdOrKey(String value) {
        this.value = Objects.requireNonNull(value, "value can't be null");
    }

    private IdOrKey(Long id) {
        this.isKey = false;
        this.id = id;
        this.value = (id != null) ? String.valueOf(id) : null;
    }

    public static IdOrKey of(String value) {
        return new IdOrKey(value);
    }

    public static IdOrKey of(Long id) {
        return new IdOrKey(id);
    }

    private void lazyInit() {
        // check is initialized ?
        if (isKey == null) {
            try {
                id = Long.parseLong(value);
                isKey = false;
            } catch (NumberFormatException e) {
                key = value;
                isKey = true;
            }
        }
    }

    public boolean isKey() {
        lazyInit();

        return isKey;
    }

    public boolean isId() {
        lazyInit();

        return !isKey();
    }

    public Long getId() {
        lazyInit();

        return id;
    }

    public String getKey() {
        lazyInit();

        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return isId() ? String.valueOf(getId()) : String.valueOf(getKey());
    }

}
