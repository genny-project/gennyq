package life.genny.bootq.sheets.module;

import life.genny.bootq.models.SheetReferralType;

/**
 * A Genny Module, not to be confused with {@link java.lang.Module}
 */
public class GennyModule extends SheetReferralType<ModuleUnit> {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GennyModule(String sheetURI) {
        super(sheetURI);
    }

    @Override
    public void init() {
        setDataUnits(getService().fetchModuleUnit(sheetURI));
    }

    @Override
    public String toString() {
        return "ModuleUnit: [name: " + name + ", sheetURI: " + sheetURI + "]";
    }
}
