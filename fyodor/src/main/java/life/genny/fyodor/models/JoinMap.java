package life.genny.fyodor.models;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.EntityEntity;

/**
 * JoinMap
 */
public class JoinMap {

	private Map<String, Join<BaseEntity, EntityAttribute>> map = new HashMap<>();
	private Join<BaseEntity, EntityEntity> linkJoin;
	private String productCode;

	public JoinMap() {
	}

	public JoinMap(String productCode) {
		this.productCode = productCode;
	}

	public Map<String, Join<BaseEntity, EntityAttribute>> getMap() {
		return map;
	}

	public void setMap(Map<String, Join<BaseEntity, EntityAttribute>> map) {
		this.map = map;
	}

	public Join<BaseEntity, EntityAttribute> get(CriteriaBuilder cb, Root<BaseEntity> baseEntity, String code) {

		// add to map if not already there
		if (!map.containsKey(code)) {
			Join<BaseEntity, EntityAttribute> join = baseEntity.join("baseEntityAttributes", JoinType.LEFT);
			join.on(cb.and(
				cb.equal(baseEntity.get("id"), join.get("pk").get("baseEntity").get("id")),
				cb.equal(join.get("pk").get("attribute").get("code"), code)
			));
			map.put(code, join);
		}

		return map.get(code);
	}

	public Join<BaseEntity, EntityEntity> getLinkJoin() {
		return linkJoin;
	}

	public void setLinkJoin(Join<BaseEntity, EntityEntity> linkJoin) {
		this.linkJoin = linkJoin;
	}

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

}
