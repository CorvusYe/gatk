package org.broadinstitute.hellbender.tools.walkers.varianteval.stratifications;

import htsjdk.variant.variantcontext.VariantContext;
import org.broadinstitute.hellbender.engine.FeatureContext;
import org.broadinstitute.hellbender.engine.ReadsContext;
import org.broadinstitute.hellbender.engine.ReferenceContext;
import org.broadinstitute.hellbender.tools.walkers.varianteval.VariantEval;
import org.broadinstitute.hellbender.tools.walkers.varianteval.evaluators.VariantEvaluator;
import org.broadinstitute.hellbender.tools.walkers.varianteval.stratifications.manager.Stratifier;
import org.broadinstitute.hellbender.tools.walkers.varianteval.util.VariantEvalSourceProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class VariantStratifier implements Comparable<VariantStratifier>, Stratifier<Object> {
    private VariantEvalSourceProvider variantEvalSourceProvider;
    final private String name;
    final protected ArrayList<Object> states = new ArrayList<Object>();

    protected VariantStratifier() {
        name = this.getClass().getSimpleName();
    }

    // -------------------------------------------------------------------------------------
    //
    // to be overloaded
    //
    // -------------------------------------------------------------------------------------

    public abstract void initialize();

    public abstract List<Object> getRelevantStates(ReferenceContext referenceContext, ReadsContext readsContext, VariantContext comp, String compName, VariantContext eval, String evalName, String sampleName, String familyName);

    // -------------------------------------------------------------------------------------
    //
    // final capabilities
    //
    // -------------------------------------------------------------------------------------

    /**
     * @return a reference to VariantEvalSourceProvider running this stratification
     */
    public final VariantEvalSourceProvider getVariantEvalSourceProvider() {
        return variantEvalSourceProvider;
    }

    /**
     * Set the VariantEvalSourceProvider.
     * @param variantEvalSourceProvider
     */
    public final void setVariantEvalSourceProvider(VariantEvalSourceProvider variantEvalSourceProvider) {
        this.variantEvalSourceProvider = variantEvalSourceProvider;
    }

    public final int compareTo(VariantStratifier o1) {
        return this.getName().compareTo(o1.getName());
    }

    @Override
    public String toString() {
        return getName();
    }

    public final String getName() {
        return name;
    }
    
    public String getFormat() { return "%s"; }
    
    public final ArrayList<Object> getAllStates() {
        return states;
    }


    /**
     * The way for a stratifier to specify that it's incompatible with specific evaluations.  For
     * example, VariantSummary includes a per-sample metric, and so cannot be used safely with Sample
     * or AlleleCount stratifications as this introduces an O(n^2) memory and cpu cost.
     *
     * @return the set of VariantEvaluators that cannot be active with this Stratification
     */
    public Set<Class<? extends VariantEvaluator>> getIncompatibleEvaluators() {
        return Collections.emptySet();
    }
}
