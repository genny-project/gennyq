package life.genny.bootq.bootxport.bootx;

public class Module extends SheetReferralType<ModuleUnit> {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Module(BatchLoadMode mode, String sheetURI) {
        super(mode, sheetURI);
    }

    @Override
    public void init() {
        setDataUnits(getService().fetchModuleUnit(sheetURI));
    }
}
