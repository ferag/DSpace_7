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
 * The purpose of this class is to denormalise received json
 * with configured fields if they are present in the json.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchDenormaliser {

    private Map<String, List<String>> mapping;

    public List<String> denormalise(String entityType, String json) {
        List<String> denormalisedJsons = new ArrayList<>();
        List<String> fields = mapping.get(entityType);
        if (Objects.isNull(fields) || fields.isEmpty()) {
            return Collections.singletonList(json);
        }
        for (String field : fields) {
            JSONObject jsonObj = new JSONObject(json);
            if (jsonObj.has(field)) {
                JSONArray array = jsonObj.getJSONArray(field);
                jsonObj.remove(field);
                if (denormalisedJsons.isEmpty()) {
                    denormaliseField(denormalisedJsons, field, jsonObj, array);
                } else {
                    List<String> tempList = new ArrayList<String>();
                    for (String j : denormalisedJsons) {
                        JSONObject toWorkJson = new JSONObject(j);
                        toWorkJson.remove(field);
                        denormaliseField(tempList, field, toWorkJson, array);
                    }
                    denormalisedJsons.clear();
                    denormalisedJsons.addAll(tempList);
                }
            }
        }
        return denormalisedJsons;
    }

    private void denormaliseField(List<String> denormalisedJsons, String field, JSONObject jsonObj, JSONArray array) {
        for (Object obj : array) {
            if (StringUtils.isNotBlank(obj.toString())) {
                JSONObject newJsonObj = new JSONObject(jsonObj.toString());
                newJsonObj.put(field, obj);
                denormalisedJsons.add(newJsonObj.toString());
            }
        }
    }

    public Map<String, List<String>> getMapping() {
        return mapping;
    }

    public void setMapping(Map<String, List<String>> mapping) {
        this.mapping = mapping;
    }

}