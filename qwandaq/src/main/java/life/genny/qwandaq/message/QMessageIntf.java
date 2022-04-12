package life.genny.qwandaq.message;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public interface QMessageIntf {
	public String getMsg_type();
}
