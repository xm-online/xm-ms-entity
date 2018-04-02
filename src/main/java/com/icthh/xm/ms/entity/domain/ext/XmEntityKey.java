package com.icthh.xm.ms.entity.domain.ext;


import com.icthh.xm.commons.exceptions.BusinessException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@link XmEntityKey} class.
 */
public final class XmEntityKey {

    private final String xmKey;
    private final String[] groups;

    private XmEntityKey(String xmKey, String[] groups) {
        this.xmKey = xmKey;
        this.groups = groups;
    }

    public static XmEntityKey ofMatcher(String xmKey, Pattern pattern) {
        Objects.requireNonNull(xmKey, "XM entity key can't be null");

        Matcher xmKeyMatcher = pattern.matcher(xmKey);
        if (!xmKeyMatcher.matches()) {
            // add more appropriate exception ...
            throw new BusinessException("XM entity key '" + xmKey + "' doesn't match pattern: "
                                                   + pattern.pattern());
        }

        int count = xmKeyMatcher.groupCount();
        String groups[] = new String[count];
        for (int i = 0; i < count; i++) {
            groups[i] = xmKeyMatcher.group(i + 1);
        }
        return new XmEntityKey(xmKey, groups);
    }

    public String getKey() {
        return xmKey;
    }

    public int getGroupsCount() {
        return (groups == null) ? 0 : groups.length;
    }

    public String getGroup(int groupIndex) {
        if (groupIndex < 0) {
            throw new IllegalArgumentException("groupIndex can't be negative");
        }

        if (groupIndex >= getGroupsCount()) {
            throw new IllegalArgumentException("Index " + groupIndex + " out of bound, groups count "
                                                   + getGroupsCount());
        }

        return groups[groupIndex];
    }

    public String[] getGroups() {
        return Arrays.copyOf(groups, getGroupsCount());
    }

    public byte getGroupAsByte(int groupIndex) {
        return Byte.parseByte(getGroup(groupIndex));
    }

    public short getGroupAsShort(int groupIndex) {
        return Short.parseShort(getGroup(groupIndex));
    }

    public int getGroupAsInt(int groupIndex) {
        return Integer.parseInt(getGroup(groupIndex));
    }

    public long getGroupAsLong(int groupIndex) {
        return Long.parseLong(getGroup(groupIndex));
    }

    public float getGroupAsFloat(int groupIndex) {
        return Float.parseFloat(getGroup(groupIndex));
    }

    public double getGroupAsDouble(int groupIndex) {
        return Float.parseFloat(getGroup(groupIndex));
    }

    public BigDecimal getGroupAsBigDecimal(int groupIndex) {
        return new BigDecimal(getGroup(groupIndex));
    }

    public BigInteger getGroupAsBigInteger(int groupIndex) {
        return new BigInteger(getGroup(groupIndex));
    }

}
