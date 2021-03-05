package org.broadinstitute.hellbender.tools.sv;

import com.google.common.collect.Ordering;
import htsjdk.samtools.SAMSequenceDictionary;
import org.broadinstitute.hellbender.engine.FeatureDataSource;
import org.broadinstitute.hellbender.tools.sv.cluster.SVClusterEngine;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitReadEvidenceAggregator extends CachingSVEvidenceAggregator<SplitReadEvidence> {

    private final int window;
    private final boolean getStart; // Retrieve start position split reads, else end position

    public SplitReadEvidenceAggregator(final FeatureDataSource<SplitReadEvidence> source,
                                       final SAMSequenceDictionary dictionary,
                                       final int window,
                                       final boolean getStart) {
        super(source, dictionary, getStart ? "StartSplitReadSites" : "EndSplitReadSites");
        this.window = window;
        this.getStart = getStart;
    }

    public int getWindow() {
        return window;
    }

    @Override
    protected SimpleInterval getEvidenceQueryInterval(final SVCallRecordWithEvidence record) {
        return (getStart ? record.getPositionAInterval() : record.getPositionBInterval()).expandWithinContig(window, dictionary);
    }

    @Override
    protected boolean evidenceFilter(final SplitReadEvidence evidence) {
        return evidence.getStrand() != getStart;
    }

    @Override
    protected SVCallRecordWithEvidence assignEvidence(final SVCallRecordWithEvidence call, final List<SplitReadEvidence> evidence) {
        Utils.nonNull(call);
        final SVCallRecordWithEvidence refinedCall;
        if (SVClusterEngine.isDepthOnlyCall(call)) {
            refinedCall = call;
        } else if (getStart) {
            final List<SplitReadSite> startSitesList = computeSites(evidence, call.getStrandA());
            refinedCall = new SVCallRecordWithEvidence(call, startSitesList, call.getEndSplitReadSites(), call.getDiscordantPairs(), call.getCopyNumberDistribution());
        } else {
            final List<SplitReadSite> endSitesList = computeSites(evidence, call.getStrandB());
            refinedCall = new SVCallRecordWithEvidence(call, call.getStartSplitReadSites(), endSitesList, call.getDiscordantPairs(), call.getCopyNumberDistribution());
        }
        if (progressMeter != null) {
            progressMeter.update(call.getPositionBInterval());
        }
        return refinedCall;
    }

    private List<SplitReadSite> computeSites(final List<SplitReadEvidence> evidenceList, final boolean strand) {
        if (!Ordering.from(IntervalUtils.getDictionaryOrderComparator(dictionary)).isOrdered(evidenceList)) {
            throw new IllegalArgumentException("Evidence list is not dictionary sorted");
        }
        final ArrayList<SplitReadSite> sites = new ArrayList<>();
        int position = 0;
        Map<String,Integer> sampleCounts = new HashMap<>();
        for (final SplitReadEvidence e : evidenceList) {
            if (e.getStart() != position) {
                if (!sampleCounts.isEmpty()) {
                    sites.add(new SplitReadSite(position, sampleCounts));
                    sampleCounts = new HashMap<>();
                }
                position = e.getStart();
            }
            if (e.getStrand() == strand && e.getCount() > 0) {
                final String sample = e.getSample();
                sampleCounts.put(sample, e.getCount());
            }
        }
        if (!sampleCounts.isEmpty()) {
            sites.add(new SplitReadSite(position, sampleCounts));
        }
        sites.trimToSize();
        return sites;
    }
}
