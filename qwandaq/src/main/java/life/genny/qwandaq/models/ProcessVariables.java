/*
 * (C) Copyright 2017 GADA Technology (http://www.outcome-hub.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * Contributors: Adam Crow , Jasper Robison
 */

package life.genny.qwandaq.models;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.entity.BaseEntity;

@RegisterForReflection
public class ProcessVariables  implements java.io.Serializable {

    private BaseEntity processEntity;
	private String definitionCode;
    private String targetCode;
    private String askMessageJson;

	public String getTargetCode() {
        return targetCode;
    }

    public void setTargetCode(String targetCode) {
        this.targetCode = targetCode;
    }

    public ProcessVariables() { }

	public String getDefinitionCode() {
        return definitionCode;
    }

    public void setDefinitionCode(String definitionCode) {
        this.definitionCode = definitionCode;
    }

    public BaseEntity getProcessEntity() {
        return processEntity;
    }

    public void setProcessEntity(BaseEntity processEntity) {
        this.processEntity = processEntity;
    }

    public String getAskMessageJson() {
        return askMessageJson;
    }

    public void setAskMessageJson(String askMessageJson) {
        this.askMessageJson = askMessageJson;
    }

}
