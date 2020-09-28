package org.dspace.importer.external.alicia.callable;

import java.util.concurrent.Callable;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.http.HttpException;
import org.dspace.importer.external.datamodel.Query;

public class SearchByQueryCallable implements Callable<String> {


    private Query query;

    private WebTarget webTarget;

    private String fields;

    public SearchByQueryCallable(String queryString, Integer maxResult, Integer start, WebTarget webTarget,
            String fields) {
        query = new Query();
        query.addParameter("query", queryString);
        query.addParameter("count", maxResult);
        query.addParameter("start", start);
        this.webTarget = webTarget;
        this.fields = fields;
    }

    public SearchByQueryCallable(Query query, WebTarget webTarget, String fields) {
        this.query = query;
        this.webTarget = webTarget;
        this.fields = fields;
    }

    @Override
    public String call() throws Exception {
        Integer start = query.getParameterAsClass("start", Integer.class);
        Integer count = query.getParameterAsClass("count", Integer.class);
        int page = start / count;
        WebTarget localTarget = webTarget.queryParam("type", "AllField");
        //page looks 1 based (start = 0, count = 20 -> page = 0)
        localTarget = localTarget.queryParam("page", page + 1);
        localTarget = localTarget.queryParam("limit", count);
        localTarget = localTarget.queryParam("prettyPrint", true);
        localTarget = localTarget.queryParam("lookfor", query.getParameterAsClass("query", String.class));
        if (fields != null && !fields.isEmpty()) {
            localTarget = localTarget.queryParam("field[]", fields);
        }
        Invocation.Builder invocationBuilder = localTarget.request();
        Response response = invocationBuilder.get();
        if (response.getStatus() == 200) {
            return response.readEntity(String.class);
        } else {
            //this exception is manager by the caller
            throw new HttpException();
        }
    }

}
