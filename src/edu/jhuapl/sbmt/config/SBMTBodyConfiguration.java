package edu.jhuapl.sbmt.config;

import edu.jhuapl.saavtk.config.Configurable;
import edu.jhuapl.saavtk.config.Entry;
import edu.jhuapl.saavtk.config.ExtensibleTypedLookup;
import edu.jhuapl.saavtk.config.FixedTypedLookup;
import edu.jhuapl.saavtk.config.Key;

public class SBMTBodyConfiguration extends ExtensibleTypedLookup implements Configurable
{
    // Required keys.
    public static final Key<String> BODY_NAME = Key.of("Body name");
    public static final Key<String> BODY_TYPE = Key.of("Body type");
    public static final Key<String> BODY_POPULATION = Key.of("Body population");

    private static final Key<FixedTypedLookup.Builder> BUILDER_KEY = Key.of("SBMTBodyConfiguration builder");

    public static Builder<SBMTBodyConfiguration> builder(String bodyName, String bodyType, String bodyPopulation) {
        final FixedTypedLookup.Builder fixedBuilder = FixedTypedLookup.builder(BUILDER_KEY);

        fixedBuilder.put(Entry.of(BODY_NAME, bodyName));
        fixedBuilder.put(Entry.of(BODY_TYPE, bodyType));
        fixedBuilder.put(Entry.of(BODY_POPULATION, bodyPopulation));

        return new Builder<SBMTBodyConfiguration>(fixedBuilder) {
            @Override
            public SBMTBodyConfiguration build()
            {
                return new SBMTBodyConfiguration(fixedBuilder);
            }
        };
    }

    protected SBMTBodyConfiguration(FixedTypedLookup.Builder builder)
    {
        super(BUILDER_KEY, builder);
    }

}
