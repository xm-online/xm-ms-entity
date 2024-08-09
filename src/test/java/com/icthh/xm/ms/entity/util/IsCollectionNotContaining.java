package com.icthh.xm.ms.entity.util;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.core.IsEqual;

public class IsCollectionNotContaining<T> extends TypeSafeDiagnosingMatcher<Iterable<? super T>> {
    private final Matcher<? super T> elementMatcher;

    public IsCollectionNotContaining(Matcher<? super T> elementMatcher) {
        this.elementMatcher = elementMatcher;
    }

    protected boolean matchesSafely(Iterable<? super T> collection, Description mismatchDescription) {

        for (var item: collection) {
            if (this.elementMatcher.matches(item)) {
                this.elementMatcher.describeMismatch(item, mismatchDescription);
                return false;
            }
        }

        return true;
    }

    public void describeTo(Description description) {
        description.appendText("a collection not containing ").appendDescriptionOf(this.elementMatcher);
    }

//    @Factory
    public static <T> Matcher<Iterable<? super T>> hasNotItem(T item) {
        return new IsCollectionNotContaining<T>(IsEqual.equalTo(item));
    }

}
