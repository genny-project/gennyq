package life.genny.qwandaq.managers.capabilities.role;

import java.util.ArrayList;
import java.util.List;

import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.clause.Or;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.entity.search.trait.Trait;


public class RoleDropdownBuilder {
    private static final String SBE_SER_LNK_ROLE = "SBE_SER_LNK_ROLE";
    
    private String name;

    private final List<Filter> filters = new ArrayList<>();

    public RoleDropdownBuilder(String name) {
        this.name = name;
    }

    public RoleDropdownBuilder addRole(String roleCode, Capability... requirements) {
        roleCode = RoleManager.cleanRoleCode(roleCode);
        Filter f = new Filter(Attribute.PRI_CODE, Operator.EQUALS, roleCode);
        if(requirements == null || requirements.length == 0) {
            filters.add(f);
            return this;
        }

        Trait.Decorator<Filter> decorator = Trait.decorator(f);
        for(Capability cap : requirements) {
            decorator.addCapabilityRequirement(cap);
        }

        filters.add(decorator.build());
        return this;

    }

    public SearchEntity build() {
        return new SearchEntity(SBE_SER_LNK_ROLE, name)
            .add(new Filter(Attribute.LNK_DEF, Operator.CONTAINS, Definition.DEF_ROLE))
            .add(new Or(filters.toArray(new Filter[0])));
    }
}
