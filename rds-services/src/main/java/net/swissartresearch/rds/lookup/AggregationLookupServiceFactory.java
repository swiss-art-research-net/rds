package net.swissartresearch.rds.lookup;

import com.metaphacts.lookup.api.LookupService;
import com.metaphacts.lookup.impl.AbstractLookupServiceFactory;

public class AggregationLookupServiceFactory extends AbstractLookupServiceFactory<AggregationLookupConfig> {
    public static final String LOOKUP_TYPE = "rds:aggregationLookup";

    public AggregationLookupServiceFactory() {
        super(LOOKUP_TYPE, AggregationLookupConfig.class);
    }

    @Override
    protected LookupService createLookupService(AggregationLookupConfig config) {
        return new AggregationLookupService(config);
    }
}
