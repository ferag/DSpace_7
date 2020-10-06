/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authenticate.service;

import java.util.ArrayList;
import java.util.List;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;

public class OidcSpecialGroupMapper {


    public List<String> jsonPathsToSpecialGroup;


    public void setJsonPathsToSpecialGroup(List<String> jsonPathsToSpecialGroup) {
        this.jsonPathsToSpecialGroup = jsonPathsToSpecialGroup;
    }


    public List<String> getSpecialGroup(String introspectResponseBody) {
        List<String> plainGroup = new ArrayList<>();
        DocumentContext document = JsonPath.parse(introspectResponseBody);
        for (String jsonPath : jsonPathsToSpecialGroup) {
            Object o = document.read(jsonPath);
            if (o.getClass().isAssignableFrom(JSONArray.class)) {
                JSONArray array = (JSONArray)o;
                int size = array.size();
                for (int index = 0; index < size; index++) {
                    plainGroup.add(array.get(index).toString());
                }
            } else {
                plainGroup.add(o.toString());
            }
        }
        return plainGroup;
    }

}
