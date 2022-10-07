package edu.jhuapl.sbmt.config;

// Data used to construct shape model (either images, radar, lidar, or enhanced)
public enum ShapeModelDataUsed
{
    ENHANCED("Enhanced"),
    IMAGE_BASED("Image-based"),
    LIDAR_BASED("Lidar-based"),
    RADAR_BASED("Radar-based"),
    WGS84("WGS84"),
    TRIAXIAL("Triaxial"),
    SIMULATED("Simulated"),
    NA("Not applicable")
    ;

    final private String str;
    private ShapeModelDataUsed(String str)
    {
        this.str = str;
    }

    @Override
    public String toString()
    {
        return str;
    }

    public static ShapeModelDataUsed valueFor(String desc)
    {
    	for (ShapeModelDataUsed type : values())
    	{
    		if (type.toString().equals(desc)) return type;
    	}
    	return null;
    }
}