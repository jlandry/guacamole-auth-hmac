package com.dmwl.guacamole.net.encryptedurl;

import org.junit.*;
import static org.junit.Assert.*;

import com.dmwl.guacamole.net.encryptedurl.Security;

public class SecurityTest {

	private Security security;
	
	@Test
	public void testDecryptCall() {
		String example = "4.size,4.1024,3.768,2.96;5.audio,9.audio/ogg;5.video;5.image,9.image/png,10.image/jpeg;7.connect,9.localhost,4.5900,0.,0.,0.;";

		System.out.println(example);
		String Encrypted = security.encrypt(example);
		System.out.println(Encrypted);
		String Decrypted = security.decrypt(Encrypted);
		System.out.println(Decrypted);
		assertEquals("failure - Decryption doesn't match",
				example, Decrypted);
	}

	@Test
	public void testEncryptDecrypt() {
		String example = "This is an example string.";

		System.out.println(example);
		String Encrypted = security.encrypt(example);
		System.out.println(Encrypted);
		String Decrypted = security.decrypt(Encrypted);
		System.out.println(Decrypted);
		assertEquals("failure - Decryption doesn't match",
				example, Decrypted);
	}

}
