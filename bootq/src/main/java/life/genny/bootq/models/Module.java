package life.genny.bootq.models;

public class Module extends SheetReferralType<ModuleUnit> {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Module(String sheetURI) {
        super(sheetURI);
    }

    @Override
    public void init() {
        setDataUnits(getService().fetchModuleUnit(sheetURI));
    }
}
