package life.genny.qwandaq.serialization.datatype;

import java.time.LocalDateTime;

import life.genny.qwandaq.CoreEntity;
import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/*
 * A representation of DataType in the cache
 * 
 * @author Varun Shastry
 */
public class DataType implements CoreEntitySerializable {

	@ProtoField(1)
	private String realm;

	@ProtoField(2)
	private String dttCode;

	@ProtoField(3)
	private String className;

	@ProtoField(4)
	private String typeName;

	@ProtoField(5)
	private String component;

	@ProtoField(6)
	private String inputMask;

	@ProtoField(7)
	private String validationCodes;

	@ProtoFactory
	public DataType(String realm, String dttCode, String className, String typeName, String component, String inputMask) {
		super();
		this.realm = realm;
		this.dttCode = dttCode;
		this.className = className;
		this.typeName = typeName;
		this.component = component;
		this.inputMask = inputMask;
	}

	public DataType() {
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getDttCode() {
		return dttCode;
	}

	public void setDttCode(String dttCode) {
		this.dttCode = dttCode;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public String getInputMask() {
		return inputMask;
	}

	public void setInputMask(String inputMask) {
		this.inputMask = inputMask;
	}

	public String getValidationCodes() {
		return validationCodes;
	}

	public void setValidationCodes(String validationCodes) {
		this.validationCodes = validationCodes;
	}

	@Override
	public CoreEntityPersistable toPersistableCoreEntity() {
		life.genny.qwandaq.datatype.DataType dataType = new life.genny.qwandaq.datatype.DataType();
		dataType.setRealm(getRealm());
		dataType.setDttCode(getDttCode());
		dataType.setClassName(getClassName());
		dataType.setTypeName(getTypeName());
		dataType.setComponent(getComponent());
		dataType.setInputmask(getInputMask());
		dataType.setValidationCodes(getValidationCodes());
		return dataType;
	}

}
