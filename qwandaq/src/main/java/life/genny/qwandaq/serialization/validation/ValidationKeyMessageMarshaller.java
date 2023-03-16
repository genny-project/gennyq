package life.genny.qwandaq.serialization.validation;

import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class ValidationKeyMessageMarshaller implements MessageMarshaller<ValidationKey> {

	@Override
	public Class<ValidationKey> getJavaClass() {
		return ValidationKey.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.persistence.validation.ValidationKey";
	}

	@Override
	public ValidationKey readFrom(ProtoStreamReader reader) throws IOException {
		String productCode = reader.readString("realm");
		String beCode = reader.readString("code");
		ValidationKey bek = new ValidationKey(productCode, beCode);
		return bek;
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, ValidationKey validationKey) throws IOException {
		writer.writeString("realm", validationKey.getRealm());
		writer.writeString("code", validationKey.getCode());
	}

}
