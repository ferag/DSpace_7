/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

/**
 * Enum to model all the relations involved in the CONCYTEC workflow.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public enum ConcytecWorkflowRelation {

    SHADOW_COPY("hasShadowCopy", "isShadowCopy"),
    CORRECTION("isCorrectionOfItem", "isCorrectedByItem"),
    WITHDRAW("isWithdrawOfItem", "isWithdrawnByItem"),
    REINSTATE("isReinstatementOfItem", "isReinstatedByItem"),
    MERGED("isMergedIn", "isMergeOf"),
    ORIGINATED("isOriginatedFrom", "isOriginOf"),
    CLONE("isCloneOfItem", "isClonedByItem");

    private final String leftType;

    private final String rightType;

    private ConcytecWorkflowRelation(String leftType, String rightType) {
        this.leftType = leftType;
        this.rightType = rightType;
    }

    public static boolean isCorrectionOrWithdrawOrReinstateRelationshipType(String type) {
        return WITHDRAW.leftType.equals(type) || REINSTATE.leftType.equals(type) || CORRECTION.leftType.equals(type);
    }

    public String getLeftType() {
        return leftType;
    }

    public String getRightType() {
        return rightType;
    }
}
