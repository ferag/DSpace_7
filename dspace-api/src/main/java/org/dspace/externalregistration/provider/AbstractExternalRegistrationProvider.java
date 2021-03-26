package org.dspace.externalregistration.provider;

import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractExternalRegistrationProvider implements ExternalRegistrationProvider {

    @Autowired(required = true)
    private EPersonService ePersonService;

    public EPersonService getePersonService() {
        return ePersonService;
    }

    public void setePersonService(EPersonService ePersonService) {
        this.ePersonService = ePersonService;
    }

}
