package edu.jhuapl.sbmt.config;

import edu.jhuapl.saavtk.config.Configurable;
import edu.jhuapl.saavtk.config.Entry;
import edu.jhuapl.saavtk.config.ExtensibleTypedLookup;
import edu.jhuapl.saavtk.config.FixedTypedLookup;
import edu.jhuapl.saavtk.config.Key;
import edu.jhuapl.sbmt.client.ShapeModelDataUsed;

public class ShapeModelConfiguration extends ExtensibleTypedLookup implements Configurable
{
    // Required keys.
    public static final Key<String> AUTHOR = Key.of("Author");
    public static final Key<ShapeModelDataUsed> DATA_USED = Key.of("Data used to create shape model");

    private static final Key<FixedTypedLookup.Builder> BUILDER_KEY = Key.of("ShapeModelConfiguration builder");

    public static Builder<ShapeModelConfiguration> builder(String author, ShapeModelDataUsed dataUsed)
    {
        final FixedTypedLookup.Builder fixedBuilder = FixedTypedLookup.builder(BUILDER_KEY);
        fixedBuilder.put(Entry.of(AUTHOR, author));
        fixedBuilder.put(Entry.of(DATA_USED, dataUsed));
        return new Builder<ShapeModelConfiguration>(fixedBuilder) {
            @Override
            public ShapeModelConfiguration build()
            {
                return new ShapeModelConfiguration(fixedBuilder);
            }
        };
    }
    protected ShapeModelConfiguration(FixedTypedLookup.Builder builder)
    {
        super(BUILDER_KEY, builder);
    }

}
