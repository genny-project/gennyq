package life.genny.qwandaq.graphql;

import java.io.Serializable;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ProcessInstanceVariables implements Serializable {

	public ProcessInstanceVariables() {
    }

    private String processId;

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }
}
