package com.icthh.xm.ms.entity.domain.ext;
import java.util.Objects;

/**
 * The {@link IdOrKey} class.
 */
public final class IdOrKey {

    private static final String SELF_KEY = "self";

    public static final IdOrKey SELF = IdOrKey.of(SELF_KEY);

    private final String value;
    private Long id;
    private String key;
    private Boolean isKey;

    private IdOrKey(String value, boolean forceKey) {
        if (forceKey) {
            this.isKey = true;
            this.key = value;
        }
        this.value = Objects.requireNonNull(value, "value can't be null");
    }

    private IdOrKey(Long id) {
        this.isKey = false;
        this.id = id;
        this.value = (id != null) ? String.valueOf(id) : null;
    }

    public static IdOrKey of(String value) {
        final String vValue = Objects.requireNonNull(value, "value can't be null");
        return new IdOrKey(vValue, false);
    }


    public static IdOrKey ofKey(String value) {
        final String vValue = Objects.requireNonNull(value, "key can't be null");
        return new IdOrKey(vValue, true);
    }

    public static IdOrKey of(Long id) {
        final Long vId = Objects.requireNonNull(id, "id can't be null");
        return new IdOrKey(vId);
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

    public boolean isSelf() {
        lazyInit();

        return isKey() && SELF_KEY.equalsIgnoreCase(key);
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

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IdOrKey other = (IdOrKey) obj;
        return Objects.equals(this.getKey(), other.getKey()) && Objects.equals(this.getId(), other.getId());
    }
}
