package life.genny.qwandaq.entity.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.bind.annotation.JsonbTransient;

import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.AssociatedColumn;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Trait;

public class TraitMap extends HashMap<Class<? extends Trait>, List<? extends Trait>> {

	public final static Map<Class<? extends Trait>, String> SERIALIZED_TRAIT_TYPES = new HashMap<>();
    static {
            SERIALIZED_TRAIT_TYPES.put(Column.class, Column.PREFIX);
            SERIALIZED_TRAIT_TYPES.put(AssociatedColumn.class, AssociatedColumn.PREFIX);
            SERIALIZED_TRAIT_TYPES.put(Action.class, Action.PREFIX);
    }

    @JsonbTransient
	public <T extends Trait> List<T> getList(Class<T> traitType) {
		List<T> traitList = (List<T>) get(traitType);
		if (traitList == null) {
			traitList = new ArrayList<>();
			put(traitType, traitList);
		}
		return traitList;
	}
}
