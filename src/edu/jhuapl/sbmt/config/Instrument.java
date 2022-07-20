package edu.jhuapl.sbmt.config;

// Names of instruments
public enum Instrument
{
    MSI("MSI"),
    NLR("NLR"),
    NIS("NIS"),
    AMICA("AMICA"),
    LIDAR("LIDAR"),
    FC("FC"),
    SSI("SSI"),
    OSIRIS("OSIRIS"),
    OLA("OLA"),
    ORX_OLA_HIGH("HELT"),
    ORX_OLA_LOW("LELT"),
    MAPCAM("MAPCAM"),
    POLYCAM("POLYCAM"),
    NAVCAM("NAVCAM"),
    OTES("OTES"),
    OVIRS("OVIRS"),
    IMAGING_DATA("Imaging"),
    MVIC("MVIC"),
    LEISA("LEISA"),
    LORRI("LORRI"),
    MOLA("MOLA"),
    GENERIC("GENERIC"),
    VIS("VIS"),
    SAMCAM("SAMCAM"),
    ISS("ISS"),
    ONC("ONC"),
    TIR("TIR"),
    LASER("LIDAR"),
    DRACO("DRACO"),
    LUKE("LUKE"),
    LEIA("LEIA"),
    ITS("ITS"),
    HRI("HRI"),
    MRI("MRI"),
    ;

    final private String str;
    private Instrument(String str)
    {
        this.str = str;
    }

    @Override
    public String toString()
    {
        return str;
    }

    public static Instrument valueFor(String str)
    {
    	for (Instrument inst : values())
    	{
    		if (str.equals(inst.toString())) return inst;
    	}
    	return null;
    }
}