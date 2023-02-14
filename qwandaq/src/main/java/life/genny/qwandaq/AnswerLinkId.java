package life.genny.qwandaq;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.HBaseEntity;
import java.util.Objects;

/**
 * AnswerLinkId stores information regarding the source and target BaseEntitys
 * for AnswerLink objects.
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 */
@Embeddable
@RegisterForReflection
public class AnswerLinkId implements java.io.Serializable {

  @JsonIgnore
  @ManyToOne
  private HBaseEntity source;

  @JsonIgnore
  @ManyToOne
  private HBaseEntity target;

  @JsonIgnore
  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  private Attribute attribute;

  /**
   * @return the source
   */
  public HBaseEntity getSource() {
    return source;
  }

  /**
   * @param source the source to set
   */
  public void setSource(final HBaseEntity source) {
    this.source = source;
  }

  /**
   * @return the target
   */
  public HBaseEntity getTarget() {
    return target;
  }

  /**
   * @param target the target to set
   */
  public void setTarget(final HBaseEntity target) {
    this.target = target;
  }

  /**
   * @return the attribute
   */
  public Attribute getAttribute() {
    return attribute;
  }

  /**
   * @param attribute the attribute to set
   */
  public void setAttribute(final Attribute attribute) {
    this.attribute = attribute;
  }

  public AnswerLinkId() {
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    final AnswerLinkId that = (AnswerLinkId) o;

      if (!Objects.equals(source, that.source))
          return false;
      if (!Objects.equals(target, that.target))
          return false;
      return Objects.equals(attribute, that.attribute);
  }

  /**
   * @return int
   */
  @Override
  public int hashCode() {
    int result;
    result = (source != null ? source.hashCode() : 0);
    result = 31 * result + (target != null ? target.hashCode() : 0);
    result = 127 * result + (attribute != null ? attribute.hashCode() : 0);
    return result;
  }

}
