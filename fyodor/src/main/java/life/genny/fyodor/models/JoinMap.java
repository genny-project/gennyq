package life.genny.fyodor.models;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;

/**
 * JoinMap
 */
public class JoinMap {

	private Map<String, Join<BaseEntity, EntityAttribute>> map = new HashMap<>();
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
			System.out.println("Creating new join");
			Join<BaseEntity, EntityAttribute> join = baseEntity.join("baseEntityAttributes", JoinType.LEFT);
			join.on(cb.equal(baseEntity.get("id"), join.get("pk").get("baseEntity").get("id")));
			map.put(code, join);
		}

		return map.get(code);
	}

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

}
