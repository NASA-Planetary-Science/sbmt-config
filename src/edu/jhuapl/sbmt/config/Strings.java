package edu.jhuapl.sbmt.config;

public enum Strings
{
	INFOFILENAMES("InfofileNames"),
	SUMFILENAMES("SumfileNames"),
	SPECTRUM_NAMES("SpectrumNames");
	
	private String name;
	
	private Strings(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
}
