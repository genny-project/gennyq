package life.genny.qwandaq.serialization.attribute;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class AttributeKeyMessageMarshaller implements MessageMarshaller<AttributeKey> {

	@Override
	public Class<AttributeKey> getJavaClass() {
		return AttributeKey.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.persistence.attribute.AttributeKey";
	}

	@Override
	public AttributeKey readFrom(ProtoStreamReader reader) throws IOException {
		String realm = reader.readString("realm");
		String code = reader.readString("code");
		return new AttributeKey(realm, code);
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, AttributeKey ak) throws IOException {
		writer.writeString("realm", ak.getRealm());
		writer.writeString("code", ak.getAttributeCode());
	}

}
