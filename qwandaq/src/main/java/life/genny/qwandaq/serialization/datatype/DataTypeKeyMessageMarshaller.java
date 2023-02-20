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
		String beCode = reader.readString("dttcode");
		DataTypeKey bek = new DataTypeKey(productCode, beCode);
		return bek;
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, DataTypeKey bek) throws IOException {
		writer.writeString("realm", bek.getRealm());
		writer.writeString("dttcode", bek.getDttCode());
	}

}
