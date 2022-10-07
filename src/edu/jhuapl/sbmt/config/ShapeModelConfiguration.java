package edu.jhuapl.sbmt.config;

import edu.jhuapl.saavtk.config.Configurable;
import edu.jhuapl.saavtk.config.ExtensibleTypedLookup;
import edu.jhuapl.saavtk.config.FixedTypedLookup;
import edu.jhuapl.saavtk.config.Key;

public class ShapeModelConfiguration extends ExtensibleTypedLookup implements Configurable
{
    // Required keys.
    public static final Key<String> AUTHOR = Key.of("Author");
    public static final Key<ShapeModelDataUsed> DATA_USED = Key.of("Data used to create shape model");

    public static Builder<ShapeModelConfiguration> builder(String author, ShapeModelDataUsed dataUsed)
    {
        final FixedTypedLookup.Builder fixedBuilder = FixedTypedLookup.builder();
        fixedBuilder.put(AUTHOR, author);
        fixedBuilder.put(DATA_USED, dataUsed);
        return new Builder<ShapeModelConfiguration>(fixedBuilder) {
            @Override
            public ShapeModelConfiguration doBuild()
            {
                return new ShapeModelConfiguration(fixedBuilder);
            }
        };
    }
    protected ShapeModelConfiguration(FixedTypedLookup.Builder builder)
    {
        super(builder);
    }

}
