package uk.org.smithfamily.mslogger.ecuDef;

public interface MSControllerInterface
{    void registerOutputChannel(OutputChannel o);

    boolean isSet(String string);

    byte[] loadPage(int i, int j, int k, byte[] bs, byte[] bs2);

    int[] loadByteVector(byte[] pageBuffer, int offset, int width, double scale, double translate, double digits,  boolean signed);

    int[][] loadByteArray(byte[] pageBuffer, int offset, int width, int height,double scale, double translate, double digits,  boolean signed);

    int[] loadWordVector(byte[] pageBuffer, int offset, int width,double scale, double translate, double digits,  boolean signed);

    int[][] loadWordArray(byte[] pageBuffer, int offset, int width, int height, double scale, double translate, double digits, boolean signed);

    double round(double x);

    int table(double x, String t);

    double timeNow();

    double tempCvt(double x);

}
