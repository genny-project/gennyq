package life.genny.bootq.models;

public class Realm extends SheetReferralType<RealmUnit> {

    public Realm(String sheetURI) {
        super(sheetURI);
    }

    @Override
    public void init() {
        setDataUnits(getService().fetchRealmUnit(sheetURI));
    }
}
