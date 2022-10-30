package life.genny.qwandaq.serialization.entityentity;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class EntityEntityKeyMessageMarshaller implements MessageMarshaller<EntityEntityKey> {

	@Override
	public Class<EntityEntityKey> getJavaClass() {
		return EntityEntityKey.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.persistence.entityentity.EntityEntityKey";
	}

	@Override
	public EntityEntityKey readFrom(ProtoStreamReader reader) throws IOException {
		String productCode = reader.readString("realm");
		String baseEntityCode = reader.readString("baseEntityCode");
		String attributeCode = reader.readString("attributeCode");
		return new EntityEntityKey(productCode, baseEntityCode, attributeCode);
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, EntityEntityKey eek) throws IOException {
		writer.writeString("realm", eek.getRealm());
		writer.writeString("baseEntityCode", eek.getBaseEntityCode());
		writer.writeString("attributeCode", eek.getAttributeCode());
	}

}
