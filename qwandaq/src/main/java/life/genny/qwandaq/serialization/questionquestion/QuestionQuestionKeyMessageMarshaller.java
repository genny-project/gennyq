package life.genny.qwandaq.serialization.questionquestion;

import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class QuestionQuestionKeyMessageMarshaller implements MessageMarshaller<QuestionQuestionKey> {

	@Override
	public Class<QuestionQuestionKey> getJavaClass() {
		return QuestionQuestionKey.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.persistence.questionquestion.QuestionQuestionKey";
	}

	@Override
	public QuestionQuestionKey readFrom(ProtoStreamReader reader) throws IOException {
		String productCode = reader.readString("realm");
		String parentCode = reader.readString("sourceCode");
		String childCode = reader.readString("targetCode");
		QuestionQuestionKey bek = new QuestionQuestionKey(productCode, parentCode, childCode);
		return bek;
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, QuestionQuestionKey qqk) throws IOException {
		writer.writeString("realm", qqk.getRealm());
		writer.writeString("sourceCode", qqk.getParentCode());
		writer.writeString("targetCode", qqk.getParentCode());
	}

}
