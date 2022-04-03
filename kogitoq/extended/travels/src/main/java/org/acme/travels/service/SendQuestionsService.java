/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.acme.travels.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.HttpUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.serviceq.Service;

@ApplicationScoped
public class SendQuestionsService {

    private static final Logger log = Logger.getLogger(SendQuestionsService.class);

    Jsonb jsonb = JsonbBuilder.create();
    @Inject
    QuestionUtils questionUtils;

    @Inject
    DatabaseUtils databaseUtils;

    @Inject
    EntityManager entityManager;

    @Inject
    Service service;

    public void sendQuestions(final String questionCode, final String sourceCode,
            final String targetCode, final String userTokenStr) {

        // service.fullServiceInit();

        GennyToken userToken = new GennyToken(userTokenStr); // work out how to pass the userToken directly
        log.info("Sending Question using Service! " + userToken.getUsername() + " : " + questionCode + ":" + sourceCode
                + ":" + targetCode);

        QDataAskMessage msg = null;

        log.info("ServiceToken = " + service.getServiceToken().getToken());

        BaseEntityUtils beUtils = new BaseEntityUtils(service.getServiceToken(), userToken);

        BaseEntity source = null;
        BaseEntity target = null;

        // source = beUtils.getBaseEntityByCode(sourceCode);
        // target = beUtils.getBaseEntityByCode(targetCode);

        source = entityManager.createQuery(
                "SELECT u from BaseEntity u WHERE u.code = :code", BaseEntity.class)
                .setParameter("code", sourceCode)
                .getSingleResult();

        target = entityManager.createQuery(
                "SELECT u from BaseEntity u WHERE u.code = :code", BaseEntity.class)
                .setParameter("code", targetCode)
                .getSingleResult();

        if (source == null) {
            log.error("Source BE not found for original " + sourceCode);
            source = beUtils.getBaseEntityByCode(beUtils.getGennyToken().getUserCode());
            if (source == null) {
                log.error("Source BE not found for userToken sourceCode" + beUtils.getGennyToken().getUserCode());
                // return null; // run exception
            }
        }

        log.info("usercode = " + userToken.getUserCode() + " usernamer=[" + userToken.getUsername() + "]");

        // Fetch the Asks

        msg = this.getQDataAskMessage(beUtils, questionCode, source, target);

        questionUtils.sendQuestions(msg, target, userToken);

        // return msg;

    }

    Question getQuestion(final String realm, final String questionCode) {
        Question question = null;
        try {

            question = entityManager
                    .createQuery(
                            "FROM Question WHERE realm=:realmStr AND code = :code",
                            Question.class)
                    .setParameter("realmStr", realm)
                    .setParameter("code", questionCode)
                    .getSingleResult();

        } catch (NoResultException e) {
            log.error("No Question found in DB for " + questionCode);
        }

        return question;
    }

    public QDataAskMessage getQDataAskMessage(BaseEntityUtils beUtils,
            String questionGroupCode,
            BaseEntity sourceBE,
            BaseEntity targetBE) {
        log.info("Got to getQuestions API");
        log.info("Sending Question using Service!  : " + questionGroupCode + ":" + sourceBE.getCode()
                + ":" + targetBE.getCode());

        Question rootQuestion = getQuestion(beUtils.getGennyToken().getRealm(), questionGroupCode);

        // test with testuser and testuser

        List<Ask> asks = questionUtils.findAsks2(rootQuestion, sourceBE, targetBE, beUtils);

        QDataAskMessage msg = new QDataAskMessage(asks.toArray(new Ask[0]));
        msg.setToken(beUtils.getGennyToken().getToken());

        return msg;
    }
}
