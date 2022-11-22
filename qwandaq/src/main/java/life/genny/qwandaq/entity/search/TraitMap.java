package life.genny.qwandaq.entity.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.AssociatedColumn;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Sort;
import life.genny.qwandaq.utils.collections.BiDirectionalHashMap;

import life.genny.qwandaq.entity.search.trait.Trait;
public class TraitMap extends HashMap<Integer, List<? extends Trait>> {
    public static BiDirectionalHashMap<Integer, Class<? extends Trait>> TRAIT_MAP_IDS = new BiDirectionalHashMap<>();
    static {
        TRAIT_MAP_IDS.put(0, Action.class);
        TRAIT_MAP_IDS.put(1, AssociatedColumn.class);
        TRAIT_MAP_IDS.put(2, Column.class);
        TRAIT_MAP_IDS.put(3, Filter.class);
        TRAIT_MAP_IDS.put(4, Sort.class);
    }

	public <T extends Trait> List<T> get(Class<T> traitType) {
		int id = TRAIT_MAP_IDS.getKey(traitType);
		List<T> list = (List<T>) super.get(id);
		if(list == null) {
			list = new ArrayList<T>();
			put(TRAIT_MAP_IDS.getKey(traitType), list);
		}
		return list;
	}

	public <T extends Trait> List<? extends Trait> put(Class<T> traitType, List<T> list) {
		int id = TRAIT_MAP_IDS.getKey(traitType);
		return put(id, list);
	}

	public <T extends Trait> void add(T trait) {
		add((Class<T>)trait.getClass(), trait);
	}

	private <T extends Trait> void add(Class<T> traitType, T trait) {
		List<T> list = (List<T>) get(traitType);
		list.add(trait);		
	}

}
