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

package life.genny.qwandaq.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ProcessBeAndDef  implements java.io.Serializable {

    public ProcessBeAndDef() {
    }

   public  ProcessBeAndDef(final BaseEntity processBE, final String defCode) {
        this.processBE = processBE;
        this.defCode = defCode;
    }

    public BaseEntity processBE;
    public String defCode;

    public BaseEntity getProcessBE() {
        return processBE;
    }

    public void setProcessBE(BaseEntity processBE) {
        this.processBE = processBE;
    }

    public String getDefCode() {
        return defCode;
    }

    public void setDefCode(String defCode) {
        this.defCode = defCode;
    }

}