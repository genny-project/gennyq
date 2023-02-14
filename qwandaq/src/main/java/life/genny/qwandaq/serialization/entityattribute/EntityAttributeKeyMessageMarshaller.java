package life.genny.qwandaq.serialization.entityattribute;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class EntityAttributeKeyMessageMarshaller implements MessageMarshaller<EntityAttributeKey> {

	@Override
	public Class<EntityAttributeKey> getJavaClass() {
		return EntityAttributeKey.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.persistence.entityattribute.EntityAttributeKey";
	}

	@Override
	public EntityAttributeKey readFrom(ProtoStreamReader reader) throws IOException {
		String productCode = reader.readString("realm");
		String baseEntityCode = reader.readString("baseEntityCode");
		String attributeCode = reader.readString("attributeCode");
		return new EntityAttributeKey(productCode, baseEntityCode, attributeCode);
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, EntityAttributeKey beak) throws IOException {
		writer.writeString("realm", beak.getRealm());
		writer.writeString("baseEntityCode", beak.getBaseEntityCode());
		writer.writeString("attributeCode", beak.getAttributeCode());
	}

}
