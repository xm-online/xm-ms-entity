package com.icthh.xm.ms.entity.service.search.aggregation;

import org.apache.lucene.util.BytesRef;

public interface DocValueFormat {

    Object format(long value);

    Object format(double value);

    Object format(BytesRef value);
}
