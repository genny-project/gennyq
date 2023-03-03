package life.genny.bootq.models;

import java.util.ArrayList;
import java.util.List;

public abstract class SheetReferralType<T> {

    protected List<T> units = new ArrayList<>();

    private ImportService service;

    public final String sheetURI;

    public SheetReferralType(String sheetURI) {
        this.sheetURI = sheetURI;
        setService(new ImportService(SheetState.getState()));
        init();
    }

    public abstract void init();

    public List<T> getDataUnits() {
        return units;
    }

    public void setDataUnits(List<T> units) {
        this.units = units;
    }

    public ImportService getService() {
        return service;
    }

    public void setService(ImportService service) {
        this.service = service;
    }


}
