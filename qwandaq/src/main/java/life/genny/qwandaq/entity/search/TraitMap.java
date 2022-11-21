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

	public final static List<Class<? extends Trait>> SERIALIZED_TRAIT_TYPES = new ArrayList<>();
    static {
            SERIALIZED_TRAIT_TYPES.add(Column.class);
            SERIALIZED_TRAIT_TYPES.add(AssociatedColumn.class);
            SERIALIZED_TRAIT_TYPES.add(Action.class);
    }

	public <T extends Trait> List<T> get(Class<T> traitType) {
		int id = TRAIT_MAP.getKey(traitType);
		List<T> traitList = (List<T>) super.get(id);
		if (traitList == null) {
			traitList = new ArrayList<>();
			put(id, traitList);
		}
		return traitList;
	}

	public <T extends Trait> List<? extends Trait> put(Class<T> traitType, List<? extends Trait> list) {
		int id = TRAIT_MAP.getKey(traitType);
		return super.put(id, list);
	}
}
