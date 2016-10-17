package com.dmwl.guacamole.net.hmac;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import com.stephensugden.guacamole.net.hmac.Security;

public class SecurityTest {

  @Test
  public void testDecryptCall() {
	  String Key= "2a64535227111123";
	  String example = "4.size,4.1024,3.768,2.96;5.audio,9.audio/ogg;5.video;5.image,9.image/png,10.image/jpeg;7.connect,9.localhost,4.5900,0.,0.,0.;";

	  System.out.println(example);
	  String Encrypted = Security.encrypt(example, Key);
	  System.out.println(Encrypted);
	  String Decrypted = Security.decrypt(Encrypted, Key);
	  System.out.println(Decrypted);
  assertEquals("failure - Decryption doesn't match",
    		example, Decrypted);
  }

  @Test
  public void testEncryptDecrypt() {
	  String Key= "2a64535227111123";
	  String example = "This is an example string.";

	  System.out.println(example);
	  String Encrypted = Security.encrypt(example, Key);
	  System.out.println(Encrypted);
	  String Decrypted = Security.decrypt(Encrypted, Key);
	  System.out.println(Decrypted);
  assertEquals("failure - Decryption doesn't match",
    		example, Decrypted);
  }

}
