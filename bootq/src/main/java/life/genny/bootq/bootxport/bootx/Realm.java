package life.genny.bootq.bootxport.bootx;

public class Realm extends SheetReferralType<RealmUnit> {

    public Realm(BatchLoadMode mode, String sheetURI) {
        super(mode, sheetURI);
    }

    public Realm(XlsxImport service, String sheetURI) {
        this(
                service instanceof XlsxImportOnline
                        ? BatchLoadMode.ONLINE : BatchLoadMode.OFFLINE, sheetURI);
    }

    @Override
    public void init() {
        setDataUnits(getService().fetchRealmUnit(sheetURI));
    }
}
