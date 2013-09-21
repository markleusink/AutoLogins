package eu.linqed.rememberme;

/*
 * <<
 * Auto Logins for IBM Domino/ XWork server
 * Copyright 2012 Mark Leusink - http://linqed.eu
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this 
 * file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License
 * >>
 */

import java.util.Date;
import java.util.Calendar;
import java.util.Random;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import com.ibm.xsp.designer.context.XSPContext;
import com.ibm.xsp.designer.context.XSPUserAgent;

import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.NotesException;

public class Token {

	private String unid;
	private String tokenId;
	private String userName;
	private String hashedToken;
	private Date validUntil;
	
	public Token( String userName, Calendar validUntil ) {
		this.tokenId = null;
		this.userName = userName;
		this.validUntil = validUntil.getTime();
	}
	
	public Token( String unid) {
		
		try {
			
			this.unid = unid;
			
			Document docToken = Utils.getCurrentDatabaseAsSigner().getDocumentByUNID(unid);
			
			this.userName = docToken.getItemValueString("userName");
			this.tokenId = docToken.getItemValueString("tokenId");
			this.validUntil = Utils.toJavaDateSafe( (DateTime) docToken.getItemValue("validUntil").get(0) );
			this.hashedToken = docToken.getItemValueString("hashedToken");
			
			docToken.recycle();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	//saves the token document, returns the token identifier
	public void save( String token) {
		
		try {
			
			Database dbCurrent = Utils.getCurrentDatabaseAsSigner();
			
			Document docToken;
			
			if (unid==null) {
				
				//new token
				
				//create document
				docToken = dbCurrent.createDocument();
				docToken.replaceItemValue("form", "fToken");
				docToken.replaceItemValue("userName", userName);
				docToken.replaceItemValue("validUntil", Utils.getSession().createDateTime(validUntil));
				docToken.replaceItemValue("remoteIp", ( (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).getRemoteAddr() );
				
				//store tokenid
				tokenId = getRandomString(15);
				docToken.replaceItemValue("tokenId", tokenId);
				
				//set readers on token document
				//it's no use to add the owner to the token document:
				//validation of a token is done before a user is signed in so we always
				//need to use the sessionAsSigner object
				Item readers = docToken.replaceItemValue("docReaders", Configuration.ROLE_ADMIN);
				readers.setReaders(true);
				
				//store browser info
				XSPUserAgent ua = XSPContext.getXSPContext(FacesContext.getCurrentInstance()).getUserAgent();

				docToken.replaceItemValue("userAgent", ua.getUserAgent() );
				docToken.replaceItemValue("browser", ua.getBrowser() );
				docToken.replaceItemValue("browserVersion", ua.getBrowserVersion() );
				
				unid = docToken.getUniversalID();
				
			} else {
				
				docToken = dbCurrent.getDocumentByUNID(unid);
			}
			
			hashedToken = Utils.getSession().hashPassword(token);
			
			docToken.replaceItemValue("hashedToken", hashedToken );
			
			docToken.save();
			docToken.recycle();
			
		} catch (NotesException e) {
			e.printStackTrace();
		}
		
	}
	
	public boolean isExpired() {
		//check if this token is expired
		Date now = new Date();
		return now.after(validUntil);
	}
	
	private String getRandomString(int len) {
		Random rnd = new Random();
		String set = "0123456789abcdefghijklmnopqrstuvwxyz";

		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			sb.append(set.charAt(rnd.nextInt(set.length())));
		}
		return sb.toString();
	}

	
	public boolean isValidToken( String cookieToken ) {
		try {
			//check if the supplied token is valid
			boolean valid = Utils.getSession().verifyPassword(cookieToken, hashedToken);
			return valid;
		} catch (NotesException e) {
			e.printStackTrace();
			return false;
		}
	}

	public String getUnid() {
		return unid;
	}
	public String getTokenId() {
		return tokenId;
	}
	public String getUserName() {
		return userName;
	}
	public Date getValidUntil() {
		return validUntil;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("Token[");
		sb.append("tokenId:" + tokenId + ", " );
		sb.append("unid:" + unid + ", " );
		sb.append("userName:" + userName + ", " );
		sb.append("hashedToken:" + hashedToken + ", " );
		sb.append("validUntil:" + validUntil.toString() );
		sb.append("]");
		
		return sb.toString();
	}
	
}
