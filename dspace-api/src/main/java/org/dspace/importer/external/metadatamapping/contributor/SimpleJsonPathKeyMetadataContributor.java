package org.dspace.importer.external.metadatamapping.contributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

/**
 * This class allow the user to get a list of metadata from json keys.
 * Reading keys using JsonPath, for each key, the resulting object will be an HashMap like
 * this: [{"role":["author"]}], unlike value reading, which return a string or at most an array.
 * This behaviour require a specific implementation to be managed.
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class SimpleJsonPathKeyMetadataContributor extends SimpleJsonPathMetadataContributor {


    /**
     * In this method, "query" must match the
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(String fullJson) {
        Collection<MetadatumDTO> metadata = new ArrayList<>();
        Collection<String> metadataValue = new ArrayList<>();
        if (getMetadataProcessor() != null) {
            metadataValue = getMetadataProcessor().processMetadata(fullJson);
        } else {
            ReadContext ctx = JsonPath.parse(fullJson);
            String innerJson = ctx.read(getQuery(), String.class);
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = jsonParser.parse(innerJson).getAsJsonObject();
            Set<String> keys = jsonObject.keySet();
            for (String value : keys) {
                metadataValue.add(value);
            }
//            if (o.getClass().isAssignableFrom(JSONArray.class)) {
//                JSONArray results = (JSONArray)o;
//                for (int i = 0; i < results.size(); i++) {
//                    String value = results.get(i).toString();
//                    metadataValue.add(value);
//                }
//            } else {
//                metadataValue.add(o.toString());
//            }
        }
        for (String value : metadataValue) {
            MetadatumDTO metadatumDto = new MetadatumDTO();
            metadatumDto.setValue(value);
            metadatumDto.setElement(getField().getElement());
            metadatumDto.setQualifier(getField().getQualifier());
            metadatumDto.setSchema(getField().getSchema());
            metadata.add(metadatumDto);
        }
        return metadata;

    }

}
