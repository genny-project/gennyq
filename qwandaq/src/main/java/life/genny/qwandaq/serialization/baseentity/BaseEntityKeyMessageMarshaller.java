package life.genny.qwandaq.serialization.baseentity;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class BaseEntityKeyMessageMarshaller implements MessageMarshaller<BaseEntityKey> {

	@Override
	public Class<BaseEntityKey> getJavaClass() {
		return BaseEntityKey.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.persistence.baseentity.BaseEntityKey";
	}

	@Override
	public BaseEntityKey readFrom(ProtoStreamReader reader) throws IOException {
		String productCode = reader.readString("realm");
		String beCode = reader.readString("code");
		BaseEntityKey bek = new BaseEntityKey(productCode, beCode);
		return bek;
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, BaseEntityKey bek) throws IOException {
		writer.writeString("realm", bek.getRealm());
		writer.writeString("code", bek.getCode());
	}

}
