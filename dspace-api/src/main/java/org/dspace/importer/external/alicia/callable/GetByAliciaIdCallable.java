package org.dspace.importer.external.alicia.callable;

import java.util.concurrent.Callable;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.http.HttpException;

public class GetByAliciaIdCallable implements Callable<String> {

    private String id;

    private final WebTarget webTarget;

    private String fields;

    public GetByAliciaIdCallable(String id, WebTarget webTarget, String fields) {
        this.id = id;
        this.webTarget = webTarget;
        this.fields = fields;
    }

    @Override
    public String call() throws Exception {
        WebTarget localTarget = webTarget.queryParam("id", id);
        if (fields != null && !fields.isEmpty()) {
            localTarget = localTarget.queryParam("field[]", fields);
        }
        localTarget = localTarget.queryParam("prettyPrint", false);
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
