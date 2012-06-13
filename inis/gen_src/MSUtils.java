
public class MSUtils
{
    public static int getLong(byte[] ochBuffer, int i)
    {
        return getWord(ochBuffer, i) * 65536 + getWord(ochBuffer, i + 2);
    }

    public static int getWord(byte[] ochBuffer, int i)
    {

        return (getByte(ochBuffer,i) * 256 + getByte(ochBuffer,i+1));
    }

    public static int getByte(byte[] ochBuffer, int i)
    {
        return (int) ochBuffer[i] & 0xFF;
    }

    public static int getSignedLong(byte[] ochBuffer, int i)
    {
        int x = getLong(ochBuffer, i);
        if (x > 2 << 32 - 1)
        {
            x = 2 << 32 - x;
        }
        return x;
    }

    public static int getSignedByte(byte[] ochBuffer, int i)
    {

        int x = getByte(ochBuffer, i);
        if (x > 127)
        {
            x = 256 - x;
        }
        return x;
    }

    public static int getSignedWord(byte[] ochBuffer, int i)
    {
        int x = getWord(ochBuffer, i);
        if (x > 32767)
        {
            x = 32768 - x;
        }
        return x;
    }
    public static int getBits(byte[] pageBuffer, int i, int _bitLo, int _bitHi, int j)
    {
        int val = 0;
        byte b = pageBuffer[i];

        long mask = ((1 << (_bitHi - _bitLo + 1)) - 1) << _bitLo;
        val = (int) ((b & mask) >> _bitLo);

        return val;
    }
    public static double getLatitude()
    {
        return 0;
    }
    public static double getLongitude()
    {
        return 0;
    }
    public static double getSpeed()
    {
        return 0;
    }
    public static double getBearing()
    {
        return 0;
    }
    
    public static double getAccuracy()
    {
        return 0;
    }
    
    public static String getTime()
    {
    	return "10:52";
    }

    public static String getLocationLogHeader()
    {
        return "Lat\tLong\tSpeed\tBearing\tAccuracy\tGPSTime";
    }
    public static String getLocationLogRow()
    {
        return getLatitude() +"\t"+getLongitude()+"\t"+getSpeed()+"\t"+getBearing()+"\t"+getAccuracy()+"\t"+getTime();
    }
}