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
    private static BiDirectionalHashMap<Integer, Class<? extends Trait>> TRAIT_MAP = new BiDirectionalHashMap<>();
    static {
        TRAIT_MAP.put(0, Action.class);
        TRAIT_MAP.put(1, AssociatedColumn.class);
        TRAIT_MAP.put(2, Column.class);
        TRAIT_MAP.put(3, Filter.class);
        TRAIT_MAP.put(4, Sort.class);
    }

	public final static List<Class<? extends Trait>> SENDABLE_TRAIT_TYPES = new ArrayList<>();
    static {
            SENDABLE_TRAIT_TYPES.add(Column.class);
            SENDABLE_TRAIT_TYPES.add(AssociatedColumn.class);
            SENDABLE_TRAIT_TYPES.add(Action.class);
    }


	public <T extends Trait> List<T> get(Class<T> traitType) {
		int id = TRAIT_MAP.getKey(traitType);
		List<T> traitList = (List<T>) super.get(id);
		return traitList;
	}

	public <T extends Trait> List<? extends Trait> put(Class<T> traitType, List<T> list) {
		int id = TRAIT_MAP.getKey(traitType);
		return put(id, list);
	}

	public <T extends Trait> void add(Trait trait) {
		List<T> list = (List<T>) get(trait.getClass());
		if(list == null) {
			list = new ArrayList<T>();
			put(TRAIT_MAP.getKey(trait.getClass()), list);
		}
		list.add((T) trait);
	}

}
