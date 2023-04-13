package life.genny.test.qwandaq.utils.capabilities.requirements.conditionals;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import life.genny.qwandaq.datatype.capability.conditionals.EConditionalType;
import life.genny.qwandaq.datatype.capability.conditionals.RequirementCondition;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityMode;
import life.genny.qwandaq.datatype.capability.core.node.CapabilityNode;
import life.genny.qwandaq.datatype.capability.core.node.PermissionMode;
import life.genny.qwandaq.utils.CommonUtils;

public class BasicConditionalTest {
    
    @Test
    public void deserialiseTest() {
        String cap = "CAP_APPLE=[\"A:A\"]";
        Capability capability = Capability.fromString(cap);
        assertEquals("CAP_APPLE", capability.code);
        assertEquals(Set.of(new CapabilityNode(CapabilityMode.ADD, PermissionMode.ALL)), capability.nodes);
    }
    
    @Test
    public void basic1() {

        /*
         * Should evalutate to (CAP_ADMIN = A:A and CAP_BASIC = D:A) or (CAP_OTHER = V:A) 
         */
        String basicString = "CAP_ADMIN=[\"A:A\"]&&CAP_BASIC=[\"D:A\"]||CAP_OTHER=[\"V:A\"]&&CAP_DUMMY=[\"A:S\"]";

        String[] splits = basicString.split("\\|\\|");
        for(int i = 0; i < splits.length; i++) {
            String s = splits[i];
            System.err.println("performing split on " + s);
            String[] s2 = s.split("\\&\\&");
            // String[] s2 = {s};
            if(s2.length == 0)
                continue;
            int j;
            System.out.print("( ");
            for(j = 0; j < s2.length - 1; j++) {
                System.out.print(s2[j] + " && ");
            }
            if(i != splits.length - 1)
                System.out.print(s2[j] + " ) || ");
            else
                System.out.print(s2[j] + " )");
        }
    }

    @Test
    public void getOrs() {
        String orTest = "CAP_ADMIN=[\"A:A\"]&&(CAP_BASIC=[\"D:A\"]||CAP_OTHER=[\"V:A\"])&&CAP_DUMMY=[\"A:S\"]";
        List<String> orGroups = getOrGroups(orTest);
        CommonUtils.printCollection(orGroups);
    }

    /**
     * Get the different or groups for a cap string
     * @param capString
     * @return
     */
    public List<String> getOrGroups(String capString) {
        int lastIndex = 0;
        int nextIndex = capString.indexOf(EConditionalType.OR.flag);
        if(nextIndex == -1)
            return List.of(capString);
        
        int nextBracketIndex = capString.indexOf("(");
        if(nextBracketIndex != -1) {
            if(nextBracketIndex < nextIndex) {
                nextIndex = nextBracketIndex + 1;
                System.out.println("");
            }
        }
        // int lastIndex = 0;
        // ors.add(capString.substring(lastIndex, nextIndex));

        String currentPortion = capString;
        List<String> ors = new ArrayList<>();
        boolean inBrackets;

        return ors;
    }

    public RequirementCondition parseGroup(String capString) {
        List<String[]> groups = new ArrayList<>();
        String[] orGroups = capString.split(EConditionalType.OR.flag);
        String[][] groups = capString.split(EConditionalType.OR.flag).split(EConditionalType.AND.flag);
        if(orGroups.length == 1) {

        }

        /**
         * For any given cap string
         * split on or
         * then split those splits on and
         */
    }

    public RequirementCondition parseNode(String capString) {
        return new RequirementCondition(Capability.fromString(capString));
    }
}
