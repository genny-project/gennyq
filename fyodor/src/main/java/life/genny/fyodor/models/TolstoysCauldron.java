package life.genny.fyodor.models;

import life.genny.qwandaq.attribute.HEntityAttribute;
import life.genny.qwandaq.entity.HBaseEntity;
import life.genny.qwandaq.entity.HEntityEntity;
import life.genny.qwandaq.entity.search.SearchEntity;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TolstoysCauldron - A cauldron of query ingredients.
 */
public class TolstoysCauldron {

	private SearchEntity searchEntity;

	private Root<HBaseEntity> root;
	private Root<HEntityEntity> link;

	private Map<String, Join<HBaseEntity, HEntityAttribute>> joinMap = new HashMap<>();
	private Map<String, Subquery<HBaseEntity>> subqueryMap = new HashMap<>();

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

	public Root<HBaseEntity> getRoot() {
		return root;
	}

	public void setRoot(Root<HBaseEntity> root) {
		this.root = root;
	}

	public Root<HEntityEntity> getLink() {
		return link;
	}

	public void setLink(Root<HEntityEntity> link) {
		this.link = link;
	}

	public Map<String, Join<HBaseEntity, HEntityAttribute>> getJoinMap() {
		return joinMap;
	}

	public void setJoinMap(Map<String, Join<HBaseEntity, HEntityAttribute>> joinMap) {
		this.joinMap = joinMap;
	}

	public Map<String, Subquery<HBaseEntity>> getSubqueryMap() {
		return subqueryMap;
	}

	public void setSubqueryMap(Map<String, Subquery<HBaseEntity>> subqueryMap) {
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
