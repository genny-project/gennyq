package life.genny.bootq.sheets.realm;

import life.genny.bootq.models.SheetReferralType;

public class Realm extends SheetReferralType<RealmUnit> {

    public Realm(String sheetURI) {
        super(sheetURI);
    }

    @Override
    public void init() {
        setDataUnits(getService().fetchRealmUnit(sheetURI));
    }
}
