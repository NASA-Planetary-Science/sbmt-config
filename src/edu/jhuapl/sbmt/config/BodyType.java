package edu.jhuapl.sbmt.config;

// Types of bodies
public enum BodyType
{
    ASTEROID("Asteroids"),
    PLANETS_AND_SATELLITES("Planets and Satellites"),
    COMETS("Comets"),
    KBO("Kuiper Belt Objects"),
    ;

    final public String str;
    private BodyType(String str)
    {
        this.str = str;
    }

    @Override
    public String toString()
    {
        return str;
    }

    public static BodyType valueFor(String desc)
    {
    	for (BodyType type : values())
    	{
    		if (type.toString().equals(desc)) return type;
    	}
    	return null;
    }
}