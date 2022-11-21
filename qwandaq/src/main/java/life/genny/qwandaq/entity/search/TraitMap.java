package life.genny.qwandaq.entity.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;

import life.genny.qwandaq.entity.search.trait.Action;
import life.genny.qwandaq.entity.search.trait.AssociatedColumn;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Trait;
import life.genny.qwandaq.serialization.adapters.search.TraitMapAdapter;

@JsonbTypeAdapter(TraitMapAdapter.class)
public class TraitMap extends HashMap<Class<? extends Trait>, List<? extends Trait>> {

	public final static List<Class<? extends Trait>> SERIALIZED_TRAIT_TYPES = new ArrayList<>();
    static {
            SERIALIZED_TRAIT_TYPES.add(Column.class);
            SERIALIZED_TRAIT_TYPES.add(AssociatedColumn.class);
            SERIALIZED_TRAIT_TYPES.add(Action.class);
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
