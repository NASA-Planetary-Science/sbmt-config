package edu.jhuapl.sbmt.config;

// Populations
public enum ShapeModelPopulation
{
    MARS("Mars System"),
    JUPITER("Jupiter System"),
    SATURN("Saturn System"),
    NEPTUNE("Neptune System"),
    NEO("Near-Earth"),
    MAIN_BELT("Main Belt"),
    PLUTO("Pluto System"),
    EARTH("Earth System"),
    NA("Not applicable");

    final private String str;
    private ShapeModelPopulation(String str)
    {
        this.str = str;
    }

    @Override
    public String toString()
    {
        return str;
    }

    public static ShapeModelPopulation valueFor(String desc)
    {
    	for (ShapeModelPopulation type : values())
    	{
    		if (type.toString().equals(desc)) return type;
    	}
    	return null;
    }
}