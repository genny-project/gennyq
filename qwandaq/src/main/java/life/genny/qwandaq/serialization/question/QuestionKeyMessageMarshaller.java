package life.genny.qwandaq.serialization.question;

import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class QuestionKeyMessageMarshaller implements MessageMarshaller<QuestionKey> {

	@Override
	public Class<QuestionKey> getJavaClass() {
		return QuestionKey.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.persistence.question.QuestionKey";
	}

	@Override
	public QuestionKey readFrom(ProtoStreamReader reader) throws IOException {
		String productCode = reader.readString("realm");
		String beCode = reader.readString("code");
		QuestionKey bek = new QuestionKey(productCode, beCode);
		return bek;
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, QuestionKey bek) throws IOException {
		writer.writeString("realm", bek.getRealm());
		writer.writeString("code", bek.getCode());
	}

}
