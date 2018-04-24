package RSA;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.BitSet;

public class RSAOAEPVerify extends RSAOAEP
{
    private BigInteger rsaMod;
    private BigInteger publicKey;
    private int modBits;

    private byte[] M;
    private int sLen;

    private byte[] EM;
    private byte[] signature;

    public boolean signatureVerification = true;

    public RSAOAEPVerify (byte[] signature, byte[] message,BigInteger rsaMod, BigInteger publicKey) throws IOException
    {
        this.signature = signature;
        this.rsaMod = rsaMod;
        this.publicKey = publicKey;
        this.M = message;
        this.sLen = 0;
        this.modBits = rsaMod.bitLength();
        this.EM = RSAVerify();
        verifyMessage(modBits-1);
    }
    public RSAOAEPVerify (byte[] signature, byte[] message, int sLen, BigInteger rsaMod, BigInteger publicKey) throws IOException
    {
        this.signature = signature;
        this.rsaMod = rsaMod;
        this.publicKey = publicKey;
        this.M = message;
        this.sLen = sLen;
        this.modBits = rsaMod.bitLength();
        this.EM = RSAVerify();
        verifyMessage(modBits-1);
    }
    private byte[] RSAVerify ()
    {
        int emLen = ceil(modBits-1,8);
        BigInteger s = OS2IP(signature);
        BigInteger m = s.modPow(publicKey, rsaMod);
        byte[] out = I2OSP(m, emLen);

        return out;
    }
    private void verifyMessage(int emBits) throws IOException
    {
        System.out.println("\nVERIFY\n");
        int emLen = ceil(emBits,8);

        // Step 2
        byte[] mHash = sha256(M);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // Step 3 & 4
        if (emLen < mHash.length + sLen + 2) {
            setVerify(false);
            System.out.println("Em < mHash + sLen + 2");
        }
        else if (EM[EM.length-1] != (byte) 0xbc) {
            setVerify(false);
            System.out.println("No 0xbc");
        }
        // Step 5
        byte[] maskedDB = new byte[emLen-mHash.length-1];
        byte[] H = new byte[mHash.length];
        System.arraycopy(EM, 0,maskedDB, 0, maskedDB.length);
        System.arraycopy(EM, maskedDB.length, H, 0, H.length);
        formatByteToStringW(maskedDB, "maskedDB");
        formatByteToStringW(H,"H       ");

        // Step 6
        int temp = 8*emLen-emBits;
        BitSet maskedDBBitset = BitSet.valueOf(maskedDB);

        System.out.println("TemP: "+temp);
        for (int i = 0; i < 0 - temp; i++)
            if (maskedDBBitset.get(i))
                setVerify(false);

        // Step 7 & 8
        byte[] dbMask = MGF(H, emLen - mHash.length - 1, mHash.length);
        byte[] DB = xorByteArrays(maskedDB, dbMask);

        // Step 9
        BitSet DBBitset = BitSet.valueOf(DB);
        for (int i = 0; i < 0 - temp; i++)
            DBBitset.set(i, false);

        // Step 10
        temp = emLen - mHash.length - sLen - 2;
        System.out.println("temp: "+ temp);

        formatByteToStringW(DB, "DB to check");
        for (int i = 0; i < temp; i++)
            if (DB[i] != (byte) 0x0) {
                setVerify(false);
                System.out.println("At position: "+i+", it has: "+DB[i]);
            }
        if (DB[temp] != (byte) 0x01) {
            setVerify(false);
        }

        byte[] Mmark;
        if (sLen > 0)
        {
            byte[] salt = new byte[sLen];
            System.arraycopy(DB, DB.length-sLen, salt, 0, sLen);

            for (int i = 0; i < 8; i++)
                stream.write((byte) 0x0);
            stream.write( mHash );
            stream.write( salt );
        }
        else
        {
            for (int i = 0; i < 8; i++)
                stream.write((byte) 0x0);
            stream.write( mHash );
        }
        Mmark = stream.toByteArray();
        formatByteToStringW(Mmark, "extracted mMark");
        byte[] Hmark = sha256(Mmark);

        formatByteToStringW(Hmark, "Hmark");
        formatByteToStringW(H, "H   ");

        for (int j = 0; j < H.length; j++)
            if (H[j] != Hmark[j])
                setVerify(false);

    }
    private void setVerify(boolean value)
    {
        this.signatureVerification = value;
        System.out.println("Signature is " + value);
    }
    public boolean getResult() { return signatureVerification;}
}