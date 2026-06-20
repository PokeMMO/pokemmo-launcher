package com.pokemmo.launcher.util;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * @author Desu <desu@pokemmo.com>
 */
public class CryptoUtil
{
	private static final PublicKey live_public_key = getPublicKey(Base64.decode("MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAyfYQx1kSfIVGdGzcHmVVP7cbyLsMXGdLhwMnx2AD1MYgU170iFN5gHT+U248rH10L6D1UMlZK1LfCsbPkdQOir3C+8Do212NONyNm/7+ZGeIwbpy+jxEQH8Jfn4JYY7+Sn4qg249yW7DSY+XKvTOcphoXRNzSQp8u6IVj03mIw7zDA0SqMMFtnCXVP3NRmtjK1SuVVFLltFctz1Pp7f9uqgqnFlgD2l8/THnddTRM5IR6O9pbOXu7My0+Jli6+4zJgw5gQvgivYPCeess9gWRqpw66VTpMJERJYA6AIbVierAbjGmtRETRsHUOGAgo54G0oxtXXEaTWXF6n6mdgSE2Ra8q7P23stsSWU3mDNQjXO0XOhtAKQCZfvICxmsH3ed5hm8bEC5yga8z8m0vyZ71fWzP4Q3g6B+o6oDsMX1nWbV2GEHci/6nwFofgOJkLINaZfUTivAIRuxECVwjTTa7ruRNgFlA2ciGUIIke2Ev2cYzyBA4LLARky2FZiEM0VAgMBAAE="));
	private static final PublicKey test_public_key = getPublicKey(Base64.decode("MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAyNb7iGEOL8/7hBkzvSm0edntPPO6/oaXlsl+1/OKukUifNnLlx+ApInkJyPy7PK6ds6R5C8liCvEEBD6G4/TRi8riG/8pIOCFAe/bjyBoXodzSJ2NRvArW6xVxax6Dpl5/tpsIBOSQDYH/BUXVZZM+DbIAbJhgvxK3r1eth7vO+kj3VLBVzJTrYpzEUz+cW+SxukOsDzHWxuUcogCEC3tzY0M3f/jMv8yrO/xVlWTzn7orrZtg0JfBMOs59NLLPKOlfB2Dtxg517Pbg2knpnNliKYE8vXwcYEJDb1jxgicqV4sYTNwu7MPSBEnYsPpBqbwbDIRI4Nrigqr+9D6Q7LPZjbI8JUZIJ1+pyYtXkSNOnnpbBPyT5WCMrviNDgocct+/PpzlYnwGRjGusqbR+6OTY9U2rptNETyK+0kmmNqxWCxAH93MqgiJk+WonkIaA5zkRlhpstlhIBwrQiYZWY6XgOMRvrdUom8FYLYKRIwRvoTRQG92gI8+WCL8+sjCpAgMBAAE="));

	public static PublicKey getLivePublicKey()
	{
		return live_public_key;
	}

	public static PublicKey getTestPublicKey()
	{
		return test_public_key;
	}

	public static boolean verifySignature(byte[] raw, byte[] signature, PublicKey key, String sig_format)
	{
		try
		{
			Signature sig2 = Signature.getInstance(sig_format);
			sig2.initVerify(key);
			sig2.update(raw);
			return sig2.verify(signature);
		}
		catch(Exception e)
		{
			System.out.println("Exception verifying " + sig_format + " signature.");
			e.printStackTrace();
		}

		return false;
	}

	private static PublicKey getPublicKey(byte[] encoded)
	{
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
		try
		{
			return KeyFactory.getInstance("RSA").generatePublic(keySpec);
		}
		catch(NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			e.printStackTrace();
		}

		return null;
	}
}