package life.genny.fyodor.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.EntityEntity;
import life.genny.qwandaq.entity.search.SearchEntity;

/**
 * TolstoysCauldron - A cauldron of query ingredients.
 */
public class TolstoysCauldron {

	private SearchEntity searchEntity;

	private Root<BaseEntity> root;
	private Root<EntityEntity> link;

	private Map<String, Join<BaseEntity, EntityAttribute>> joinMap = new HashMap<>();
	private Map<String, Subquery<BaseEntity>> subqueryMap = new HashMap<>();

	private List<Predicate> predicates = new ArrayList<>();
	private List<Order> orders = new ArrayList<>();

	public TolstoysCauldron() {
	}

	public TolstoysCauldron(SearchEntity searchEntity) {
		this.searchEntity = searchEntity;
	}

	public SearchEntity getSearchEntity() {
		return searchEntity;
	}

	public void setSearchEntity(SearchEntity searchEntity) {
		this.searchEntity = searchEntity;
	}

	public Root<BaseEntity> getRoot() {
		return root;
	}

	public void setRoot(Root<BaseEntity> root) {
		this.root = root;
	}

	public Root<EntityEntity> getLink() {
		return link;
	}

	public void setLink(Root<EntityEntity> link) {
		this.link = link;
	}

	public Map<String, Join<BaseEntity, EntityAttribute>> getJoinMap() {
		return joinMap;
	}

	public void setJoinMap(Map<String, Join<BaseEntity, EntityAttribute>> joinMap) {
		this.joinMap = joinMap;
	}

	public Map<String, Subquery<BaseEntity>> getSubqueryMap() {
		return subqueryMap;
	}

	public void setSubqueryMap(Map<String, Subquery<BaseEntity>> subqueryMap) {
		this.subqueryMap = subqueryMap;
	}

	public List<Predicate> getPredicates() {
		return predicates;
	}

	public void setPredicates(List<Predicate> predicates) {
		this.predicates = predicates;
	}

	public List<Order> getOrders() {
		return orders;
	}

	public void setOrders(List<Order> orders) {
		this.orders = orders;
	}

	@JsonbTransient
	public String getProductCode() {
		return this.searchEntity.getRealm();
	}

	/**
	 * Add a predicate to the mix.
	 * 
	 * @param predicate
	 */
	public void add(Predicate predicate) {
		this.predicates.add(predicate);
	}

	/**
	 * Add an order to the mix.
	 * 
	 * @param order
	 */
	public void add(Order order) {
		this.orders.add(order);
	}

}
