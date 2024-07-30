package com.icthh.xm.ms.entity.service.search.deserializer;

import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpDeserializer;
import co.elastic.clients.json.ObjectBuilderDeserializer;
import co.elastic.clients.json.ObjectDeserializer;

public class PutMappingRequestDeserializer {

    public static JsonpDeserializer<PutMappingRequest> getBuilderDeserializer(PutMappingRequest.Builder builder) {
        return ObjectBuilderDeserializer.lazy(() -> builder, PutMappingRequestDeserializer::setupPutMappingRequestDeserializer);
    }

    private static void setupPutMappingRequestDeserializer(ObjectDeserializer<PutMappingRequest.Builder> op) {

        op.add(PutMappingRequest.Builder::fieldNames, FieldNamesField._DESERIALIZER, "_field_names");
        op.add(PutMappingRequest.Builder::meta, JsonpDeserializer.stringMapDeserializer(JsonData._DESERIALIZER), "_meta");
        op.add(PutMappingRequest.Builder::routing, RoutingField._DESERIALIZER, "_routing");
        op.add(PutMappingRequest.Builder::source, SourceField._DESERIALIZER, "_source");
        op.add(PutMappingRequest.Builder::dateDetection, JsonpDeserializer.booleanDeserializer(), "date_detection");
        op.add(PutMappingRequest.Builder::dynamic, DynamicMapping._DESERIALIZER, "dynamic");
        op.add(PutMappingRequest.Builder::dynamicDateFormats, JsonpDeserializer.arrayDeserializer(JsonpDeserializer.stringDeserializer()),
            "dynamic_date_formats");
        op.add(PutMappingRequest.Builder::dynamicTemplates, JsonpDeserializer.arrayDeserializer(
            JsonpDeserializer.stringMapDeserializer(DynamicTemplate._DESERIALIZER)), "dynamic_templates");
        op.add(PutMappingRequest.Builder::numericDetection, JsonpDeserializer.booleanDeserializer(), "numeric_detection");
        op.add(PutMappingRequest.Builder::properties, JsonpDeserializer.stringMapDeserializer(Property._DESERIALIZER), "properties");
        op.add(PutMappingRequest.Builder::runtime, JsonpDeserializer.stringMapDeserializer(RuntimeField._DESERIALIZER), "runtime");
    }
}
