package life.genny.qwandaq.serialization.validation;

import java.io.UncheckedIOException;

import life.genny.qwandaq.constants.GennyConstants;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.impl.ResourceUtils;

public class ValidationKeyInitializerImpl implements SerializationContextInitializer {

	@Override
	public String getProtoFileName() {
		return "validation-persistence.proto";
	}

	@Override
	public String getProtoFile() throws UncheckedIOException {
		return ResourceUtils.getResourceAsString(getClass(), GennyConstants.PATH_TO_PROTOS + getProtoFileName());
	}

	@Override
	public void registerSchema(SerializationContext serCtx) {
		serCtx.registerProtoFiles(FileDescriptorSource.fromString(getProtoFileName(), getProtoFile()));

	}

	@Override
	public void registerMarshallers(SerializationContext serCtx) {
		serCtx.registerMarshaller(new ValidationKeyMessageMarshaller());
	}

}
