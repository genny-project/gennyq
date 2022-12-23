package life.genny.bootq.bootxport.bootx;

public class Realm extends SheetReferralType<RealmUnit> {

    public Realm(String sheetURI) {
        super(sheetURI);
    }

    public Realm(XlsxImport service, String sheetURI) {
        this(sheetURI);
    }

    @Override
    public void init() {
        setDataUnits(getService().fetchRealmUnit(sheetURI));
    }
}
