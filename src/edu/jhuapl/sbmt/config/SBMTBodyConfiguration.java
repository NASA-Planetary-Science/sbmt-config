package edu.jhuapl.sbmt.config;

import edu.jhuapl.saavtk.config.Configurable;
import edu.jhuapl.saavtk.config.ExtensibleTypedLookup;
import edu.jhuapl.saavtk.config.FixedTypedLookup;
import edu.jhuapl.saavtk.config.Key;
import edu.jhuapl.sbmt.imaging.instruments.ImagingInstrumentConfiguration;

public class SBMTBodyConfiguration extends ExtensibleTypedLookup implements Configurable
{
    public static final Key<ImagingInstrumentConfiguration> IMAGING_INSTRUMENT_CONFIG = Key.of("Imaging instrument configuration");

    private static final Key<FixedTypedLookup.Builder> BUILDER_KEY = Key.of("SBMTBodyConfiguration builder");

    public static Builder<SBMTBodyConfiguration> builder() {
        FixedTypedLookup.Builder fixedBuilder = FixedTypedLookup.builder(BUILDER_KEY);
        return new Builder<SBMTBodyConfiguration>(fixedBuilder) {
            @Override
            public SBMTBodyConfiguration build()
            {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    protected SBMTBodyConfiguration(FixedTypedLookup.Builder builder)
    {
        super(BUILDER_KEY, builder);
    }

}
