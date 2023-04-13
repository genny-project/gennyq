package life.genny.qwandaq.datatype.capability.conditionals;

import java.util.LinkedList;

import life.genny.qwandaq.datatype.capability.core.Capability;

public class RequirementCondition {

    private static final String CONTAINER_START = "( ";
    private static final String CONTAINER_END = " )";
    private static final String CONTAINER_EMPTY = "( )";

    EConditionalType conditionalType;
    LinkedList<RequirementCondition> childRequirements = new LinkedList<>();
    Capability node;

    public RequirementCondition(Capability node) {
        this.node = node;
    }

    public RequirementCondition(EConditionalType conditionType, RequirementCondition... conditions) {
        this.conditionalType = conditionType;

    }

    public RequirementCondition setConditionType(EConditionalType type) {
        if(isLeafNode()) {
            throw new UnsupportedOperationException("Attempted to set the conditional type of a leaf node");
        }
        conditionalType = type;
        return this;
    }

    public RequirementCondition addChild(RequirementCondition clause) {
        if(isLeafNode()) {
            throw new UnsupportedOperationException("Attempted to add child to a non conditional requirement. Please set conditional first");
        }
        childRequirements.add(clause);
        return this;
    }

    public boolean isLeafNode() {
        return !isContainer();
    }

    public boolean isContainer() {
        return conditionalType != null;
    }

    public String toString() {
        if(isLeafNode()) {
            return this.node.toString();
        }
        if(isContainer()) {
            if(childRequirements.size() == 0){
                return CONTAINER_EMPTY;
            }
            
            int i;
            StringBuilder sb = new StringBuilder(CONTAINER_START);
            
            for(i = 0; i < childRequirements.size() - 1; i++ ) {
                sb.append(childRequirements.get(i).toString()).append(" ").append(conditionalType).append(" ");
            }

            sb.append(childRequirements.get(i).toString()).append(CONTAINER_END);
            return sb.toString();
        }

        return null;
    }
}
