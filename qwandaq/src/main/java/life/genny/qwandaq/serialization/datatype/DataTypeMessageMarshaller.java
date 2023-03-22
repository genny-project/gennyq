package life.genny.qwandaq.serialization.datatype;

import life.genny.qwandaq.datatype.DataType;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class DataTypeMessageMarshaller implements MessageMarshaller<DataType> {

	public static final String TYPE_NAME = "life.genny.qwandaq.persistence.datatype.DataType";

	@Override
	public Class<DataType> getJavaClass() {
		return DataType.class;
	}

	@Override
	public String getTypeName() {
		return TYPE_NAME;
	}

	// @Override
	public DataType readFrom(ProtoStreamReader reader) throws IOException {
		DataType dataType = new DataType();
		dataType.setRealm(reader.readString("realm"));
		dataType.setDttCode(reader.readString("dttcode"));
		dataType.setClassName(reader.readString("classname"));
		dataType.setTypeName(reader.readString("typename"));
		dataType.setComponent(reader.readString("component"));
		dataType.setInputmask(reader.readString("inputmask"));
		dataType.setValidationCodes(reader.readString("validation_codes"));
		return dataType;
	}

	// @Override
	public void writeTo(ProtoStreamWriter writer, DataType dataType) throws IOException {
		// writer.writeLong("id", dataType.getId());
		writer.writeString("realm", dataType.getRealm());
		writer.writeString("dttcode", dataType.getDttCode());
		writer.writeString("classname", dataType.getClassName());
		writer.writeString("typename", dataType.getTypeName());
		writer.writeString("component", dataType.getComponent());
		writer.writeString("inputmask", dataType.getInputmask());
		writer.writeString("validation_codes", dataType.getValidationCodes());
	}

}
