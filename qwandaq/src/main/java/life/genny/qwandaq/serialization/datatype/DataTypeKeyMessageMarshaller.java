package life.genny.qwandaq.serialization.datatype;

import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class DataTypeKeyMessageMarshaller implements MessageMarshaller<DataTypeKey> {

	@Override
	public Class<DataTypeKey> getJavaClass() {
		return DataTypeKey.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.persistence.datatype.DataTypeKey";
	}

	@Override
	public DataTypeKey readFrom(ProtoStreamReader reader) throws IOException {
		String productCode = reader.readString("realm");
		String dtCode = reader.readString("code");
		DataTypeKey dataTypeKey = new DataTypeKey(productCode, dtCode);
		return dataTypeKey;
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, DataTypeKey dataTypeKey) throws IOException {
		writer.writeString("realm", dataTypeKey.getRealm());
		writer.writeString("code", dataTypeKey.getCode());
	}

}
