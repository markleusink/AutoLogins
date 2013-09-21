package com.developi.openntf;

/**
 * LtpaToken Generator V1.0
 *
 * This Java class generates a valid LtpaToken valid for any user name.
 * 
 * @author Serdar Basegmez, Developi (http://lotusnotus.com/en)
 * 
 * This code was copied from the Xsnippet site:
 * http://openntf.org/XSnippets.nsf/snippet.xsp?id=ltpatoken-generator-for-multi-server-sso-configurations
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this 
 * file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License
 *
 * To use it on SSJS:
 * -------------------
 *  importPackage(com.developi.openntf);
 *  var ltpa:LtpaGenerator=new LtpaGenerator();
 *  ltpa.initByConfiguration(sessionAsSigner, "Developi:LtpaToken");
 *  token=ltpa.generateLtpaToken("CN=Serdar Basegmez/O=developi");
 *
 * To use the token (make sure replace '.developi.info' with your SSO domain):
 * -------------------------------------------------------------------------
 *  response=facesContext.getExternalContext().getResponse();
 *  response.setHeader("Set-Cookie", "LtpaToken=" + token + "; domain=.developi.info; path=/");
 * facesContext.getExternalContext().redirect(someUrl);
 *
 * 1. "Developi:LtpaToken" is the SSO configuration key. If you are using Internet site configuration,  it will be
 *     "Organization:TokenName". Otherwise, it will be "TokenName" only. You may check "($WebSSOConfigs)"
 *     view in the names.nsf database.
 * 2. sessionAsSigner should be given as parameter to the initByConfiguration method.
 * 3. The signer of the database design should be listed as 'Owner' or 'Administrator' in the SSO configuration.
 * 4. Current version only supports Domino keys. Tokens imported from Websphere will not generate valid tokens.
 */
 
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.GregorianCalendar;
 
import javax.xml.bind.DatatypeConverter;
 
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.Database;
import lotus.domino.View;

public class LtpaGenerator {
 
  public final String NAMESDB="names.nsf";
  public final String SSOVIEW="($WebSSOConfigs)";
  public final String SSO_DOMINO_SECRETFIELD="LTPA_DominoSecret";
  public final String SSO_DOMINO_DURATIONFIELD="LTPA_TokenExpiration";
   
  private boolean ready=false;
 
  private int duration=300;
  private String ltpaSecret="";
   
  public LtpaGenerator() {
  }
 
  public LtpaGenerator(String ltpaSecret) {
    setLtpaSecret(ltpaSecret);
  }
 
  public LtpaGenerator(String ltpaSecret, int duration) {
    setLtpaSecret(ltpaSecret);
    setDuration(duration);
  }
   
  public void initByConfiguration(Session session, String configName) throws Exception {
    Database dbNames=null;
    View ssoView=null;
    Document ssoDoc=null;
     
    try {
      String currentServer=session.getCurrentDatabase().getServer();
      dbNames=session.getDatabase(currentServer, NAMESDB, false);
      ssoView=dbNames.getView(SSOVIEW);
      ssoDoc=ssoView.getDocumentByKey(configName, true);
      if(ssoDoc==null) {
        throw new IllegalArgumentException("Unable to find SSO configuration with the given configName.");
      }
       
      setLtpaSecret(ssoDoc.getItemValueString(SSO_DOMINO_SECRETFIELD));
      setDuration(ssoDoc.getItemValueInteger(SSO_DOMINO_DURATIONFIELD));
       
    } catch (NotesException ex) {
      throw new Exception("Notes Error: "+ex);
    } finally {
      try {
        if(dbNames!=null) dbNames.recycle();
        if(ssoView!=null) ssoView.recycle();
        if(ssoDoc!=null) ssoDoc.recycle();       
      } catch(NotesException exc) {
        //ignore
      }
    }
  }
   
  public String generateLtpaToken(String userName) {
    if(!isReady()) {
      throw new IllegalStateException("LtpaGenerator is not ready.");
    }
     
    MessageDigest sha1 = null;
 
    GregorianCalendar creationDate=new GregorianCalendar();
    GregorianCalendar expiringDate=new GregorianCalendar();
   
    expiringDate.add(GregorianCalendar.MINUTE, duration);
     
    try {
      sha1 = MessageDigest.getInstance( "SHA-1" );
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace(System.err);
    }
 
    byte[] secretDecoded=DatatypeConverter.parseBase64Binary(ltpaSecret);
    byte[] tokenBase=("\000\001\002\003"+getHexRep(creationDate)+getHexRep(expiringDate)+userName).getBytes();
    byte[] digest=sha1.digest(concatBytes(tokenBase, secretDecoded));
   
    return DatatypeConverter.printBase64Binary(concatBytes(tokenBase, digest));
     
  }
 
  public static byte[] concatBytes(byte[] arr1, byte[] arr2) {
    byte[] result=Arrays.copyOf(arr1, arr1.length+arr2.length);
    System.arraycopy(arr2, 0, result, arr1.length, arr2.length);
    return result;
  }
   
  public static String getHexRep(GregorianCalendar date) {
    int timeVal=(int)(date.getTimeInMillis()/1000);
    String hex=Integer.toHexString(timeVal).toUpperCase();
     
    if(hex.length()>=8) {
      return hex;
    } else {
      return String.format("%0"+(8-hex.length())+"d", 0)+hex;
    }
  }
 
  public void setDuration(int duration) {
    this.duration = duration;
  }
 
  public void setLtpaSecret(String ltpaSecret) {
    this.ltpaSecret = ltpaSecret;
    this.ready=true;
  }
 
  public boolean isReady() {
    return ready;
  }
 
}