/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The purpose of this class is to denormalize received json
 * with configured fields if they are present in the json.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchDenormalizer {

    private Map<String, List<String>> denormalizationMapping;

    public List<String> denormalize(String entityType, String json) {
        List<String> denormalizedJsons = new ArrayList<>();
        List<String> fields = denormalizationMapping.get(entityType);
        if (Objects.isNull(fields) || fields.isEmpty()) {
            return Collections.singletonList(json);
        }
        for (String field : fields) {
            JSONObject jsonObj = new JSONObject(json);
            if (jsonObj.has(field)) {
                JSONArray array = jsonObj.getJSONArray(field);
                if (denormalizedJsons.isEmpty()) {
                    if (array.length() == 0) {

                        denormalizedJsons.add(jsonObj.toString());
                    } else {
                        denormalizeField(denormalizedJsons, field, jsonObj, array);
                    }
                }  else {
                    List<String> tempList = new ArrayList<String>();
                    for (String j : denormalizedJsons) {
                        JSONObject toWorkJson = new JSONObject(j);
                        toWorkJson.remove(field);
                        denormalizeField(tempList, field, toWorkJson, array);
                    }
                    denormalizedJsons.clear();
                    denormalizedJsons.addAll(tempList);
                }
            }
        }
        if (denormalizedJsons.isEmpty()) {
            return Collections.singletonList(json);
        }
        return denormalizedJsons;
    }

    private void denormalizeField(List<String> denormalisedJsons, String field, JSONObject jsonObj, JSONArray array) {
        for (Object obj : array) {
            if (StringUtils.isNotBlank(obj.toString())) {
                JSONObject newJsonObj = new JSONObject(jsonObj.toString());
                newJsonObj.put(field, obj);

                denormalisedJsons.add(newJsonObj.toString());
            }
        }
    }

    public Map<String, List<String>> getDenormalizationMapping() {
        return denormalizationMapping;
    }

    public void setDenormalizationMapping(Map<String, List<String>> denormalizationMapping) {
        this.denormalizationMapping = denormalizationMapping;
    }



}
