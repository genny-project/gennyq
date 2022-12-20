package life.genny.bootq.bootxport.bootx;

import java.io.Serializable;
import java.util.Set;

public class StateModel implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Set<String> sheetIDWorksheetConcatenated;

    public Set<String> getSheetIDWorksheetConcatenated() {
        return sheetIDWorksheetConcatenated;
    }

    public void setSheetIDWorksheetConcatenated(Set<String> sheetIDWorksheetConcatenated) {
        this.sheetIDWorksheetConcatenated = sheetIDWorksheetConcatenated;
    }

    @Override
    public String toString() {
        return "StateModel [sheetIDWorksheetConcatenated=" + sheetIDWorksheetConcatenated + "]";
    }
}
