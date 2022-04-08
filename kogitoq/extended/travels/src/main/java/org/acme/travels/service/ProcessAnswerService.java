
package org.acme.travels.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.serviceq.Service;

@ApplicationScoped
public class ProcessAnswerService {

    private static final Logger log = Logger.getLogger(ProcessAnswerService.class);

    Jsonb jsonb = JsonbBuilder.create();
    @Inject
    QuestionUtils questionUtils;

    @Inject
    DatabaseUtils databaseUtils;

    @Inject
    EntityManager entityManager;

    @Inject
    Service service;

    public Boolean processBaseEntity(final String qDataAnswerMessageJson, final String qDataAskMessage,
            BaseEntity processBE) {
        Boolean allMandatoryAttributesAnswered = false;
        log.info("In processBaseEntity :");
        log.info("AnswerMsg " + qDataAnswerMessageJson);

        return allMandatoryAttributesAnswered;
    }

}
