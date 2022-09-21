package life.genny.qwandaq.message;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public abstract class QDataMessage extends QMessage {

  /**
   * @return String
   */
  public String getData_type() {
    return data_type;
  }

  /**
   * @param data_type the data type to set
   */
  public void setData_type(final String data_type) {
    this.data_type = data_type;
  }

  private static final String MESSAGE_TYPE = "DATA_MSG";
  private String data_type;
  private Boolean delete = false;
  private Boolean replace = false;
  private boolean shouldDeleteLinkedBaseEntities;
  private String aliasCode;

  public QDataMessage() {

    // super();
  }

  public QDataMessage(final String data_type) {

    super(MESSAGE_TYPE);
    this.data_type = data_type;
  }

  /**
   * @return Boolean
   */
  public Boolean getDelete() {
    return delete;
  }

  /**
   * @param delete the delete status to set
   */
  public void setDelete(final Boolean delete) {
    this.delete = delete;
  }

  /**
   * @return String
   */
  @Override
  public String toString() {
    return "QDataMessage [data_type=" + data_type + ", delete=" + delete + "]";
  }

  /**
   * @return String
   */
  public String getAliasCode() {
    return aliasCode;
  }

  /**
   * @param aliasCode the aliasCode to set
   */
  public void setAliasCode(final String aliasCode) {
    this.aliasCode = aliasCode;
  }

  /**
   * @return the replace
   */
  public Boolean getReplace() {
    return replace;
  }

  /**
   * @param replace the replace to set
   */
  public void setReplace(Boolean replace) {
    this.replace = replace;
  }

  /**
   * @return the shouldDeleteLinkedBaseEntities
   */
  public Object getShouldDeleteLinkedBaseEntities() {
    return shouldDeleteLinkedBaseEntities;
  }

  /**
   * @return boolean
   */
  public boolean isShouldDeleteLinkedBaseEntities() {
    return (boolean) getShouldDeleteLinkedBaseEntities();
  }

  /**
   * @param shouldDeleteLinkedBaseEntities the shouldDeleteLinkedBaseEntities to
   *                                       set
   */
  public void setShouldDeleteLinkedBaseEntities(Object shouldDeleteLinkedBaseEntities) {
    this.shouldDeleteLinkedBaseEntities = (Boolean) shouldDeleteLinkedBaseEntities;
  }

  public Boolean isDelete() {
    return this.delete;
  }

  public Boolean isReplace() {
    return this.replace;
  }

}
