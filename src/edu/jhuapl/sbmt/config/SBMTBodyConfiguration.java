package edu.jhuapl.sbmt.config;

import edu.jhuapl.saavtk.config.Configurable;
import edu.jhuapl.saavtk.config.ExtensibleTypedLookup;
import edu.jhuapl.saavtk.config.FixedTypedLookup;
import edu.jhuapl.saavtk.config.Key;

public class SBMTBodyConfiguration extends ExtensibleTypedLookup implements Configurable
{
    // Required keys.
    public static final Key<String> BODY_NAME = Key.of("Body name");
    public static final Key<String> BODY_TYPE = Key.of("Body type");
    public static final Key<String> BODY_POPULATION = Key.of("Body population");

    public static Builder<SBMTBodyConfiguration> builder(String bodyName, String bodyType, String bodyPopulation) {
        final FixedTypedLookup.Builder fixedBuilder = FixedTypedLookup.builder();

        fixedBuilder.put(BODY_NAME, bodyName);
        fixedBuilder.put(BODY_TYPE, bodyType);
        fixedBuilder.put(BODY_POPULATION, bodyPopulation);

        return new Builder<SBMTBodyConfiguration>(fixedBuilder) {
            @Override
            public SBMTBodyConfiguration doBuild()
            {
                return new SBMTBodyConfiguration(fixedBuilder);
            }
        };
    }

    protected SBMTBodyConfiguration(FixedTypedLookup.Builder builder)
    {
        super(builder);
    }

}
