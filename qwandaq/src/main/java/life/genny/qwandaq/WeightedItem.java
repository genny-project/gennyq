package life.genny.qwandaq;

import java.io.Serializable;

import org.apache.commons.lang3.builder.CompareToBuilder;

import life.genny.qwandaq.entity.BaseEntity;

public class WeightedItem implements Serializable, Comparable<Object> {
	
	private BaseEntity item;
	private Double weight;

	public WeightedItem() {
	}
	
	/**
	 * Constructor
	 *
	 * @param item the item to set
	 * @param weight the weight to set
	 */
	public WeightedItem(BaseEntity item, Double weight) {
		this.item = item;
		this.weight = weight;
	}

	/**
	 * @return the item
	 */
	public BaseEntity getItem() {
		return item;
	}

	/**
	 * @param item the item to set
	 */
	public void setItem(BaseEntity item) {
		this.item = item;
	}

	/**
	 * @return the weight
	 */
	public Double getWeight() {
		return weight;
	}

	/**
	 * @param weight the weight to set
	 */
	public void setWeight(Double weight) {
		this.weight = weight;
	}
	
	/** 
	 * @return String
	 */
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "WeightedItem [weight=" + weight + ", item=" + item.getCode() + "]";
	}

	/** 
	 * Compare to an object
	 *
	 * @param o the object to compare to
	 * @return int
	 */
	@Override
	public int compareTo(Object o) {
		WeightedItem myItem = (WeightedItem) o;

		return new CompareToBuilder().append(this.getWeight(), myItem.getWeight()).toComparison();
	}
	
}
