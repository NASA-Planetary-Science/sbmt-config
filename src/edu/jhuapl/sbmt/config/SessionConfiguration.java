package edu.jhuapl.sbmt.config;

import edu.jhuapl.saavtk.config.Configurable;
import edu.jhuapl.saavtk.config.Entry;
import edu.jhuapl.saavtk.config.ExtensibleTypedLookup;
import edu.jhuapl.saavtk.config.FixedTypedLookup;
import edu.jhuapl.saavtk.config.Key;
import edu.jhuapl.sbmt.imaging.instruments.ImagingInstrumentConfiguration;

public class SessionConfiguration extends ExtensibleTypedLookup
        implements Configurable
{
    // Required keys.
    public static final Key<SBMTBodyConfiguration> BODY_CONFIG = Key.of("Body configuration");
    public static final Key<ShapeModelConfiguration> SHAPE_MODEL_CONFIG = Key.of("Shape model configuration");

    // Optional keys.
    public static final Key<ImagingInstrumentConfiguration> IMAGING_INSTRUMENT_CONFIG = Key.of("Imaging instrument configuration");

    private static final Key<FixedTypedLookup.Builder> BUILDER_KEY = Key.of("SessionConfiguration builder");

    public static final Builder<SessionConfiguration> builder(SBMTBodyConfiguration bodyConfig, ShapeModelConfiguration shapeConfig)
    {
        final FixedTypedLookup.Builder fixedBuilder = FixedTypedLookup.builder(BUILDER_KEY);

        fixedBuilder.put(Entry.of(BODY_CONFIG, bodyConfig));
        fixedBuilder.put(Entry.of(SHAPE_MODEL_CONFIG, shapeConfig));

        return new Builder<SessionConfiguration>(fixedBuilder) {
            @Override
            public SessionConfiguration build()
            {
                return new SessionConfiguration(fixedBuilder);
            }
        };
    }

    protected SessionConfiguration(FixedTypedLookup.Builder builder)
    {
        super(BUILDER_KEY, builder);
    }

}
