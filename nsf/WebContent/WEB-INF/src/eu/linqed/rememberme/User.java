package eu.linqed.rememberme;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import lotus.domino.NotesException;

public class User implements Serializable {

	private static final long serialVersionUID = 7115448834458628418L;
	
	private String userName;
	private String remoteIp;
	private boolean ltpaTokenSet;
	private boolean allowedIp;
	
	private static final String BEAN_NAME = "alUserBean";
	
	private Configuration config;
	
	public User() {
		
		try {
			config = Configuration.get();
			
			//retrieve the username of the current user
			String currentUser = Utils.getSession().getEffectiveUserName();
			
			//(re-)init the user bean if another user logged in
			if (!currentUser.equals(userName)) {

				userName = Utils.getSession().getEffectiveUserName();
				remoteIp = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).getRemoteAddr();
				ltpaTokenSet = false;
				allowedIp = checkAllowedIp();

				log("user bean created for " + userName + ", remote IP: " + remoteIp + " (" + (allowedIp ? "allowed to use auto logins" : "not allowed to use auto logins") + ")");
			
			}
		} catch (NotesException e) {
			e.printStackTrace();
		}
		
	}
	
	//check if the current user is allowed to use the remember me function by
	//comparing the remote IP address with a list of restricted IP addresses
	private boolean checkAllowedIp() {
		
		if (config.getAllowedIPPatterns().size() == 0) {
			return true; 		//no restrictions
			
		} else {
			
			for(String range : config.getAllowedIPPatterns()) {
				if (remoteIp.indexOf(range)==0) {
					return true;
				}
			}
			return false;
		}
		
	}
	
	//access to the user bean
	public static User get() {
		return (User) Utils.resolveVariable(BEAN_NAME);
	}
	
	public String getRemoteIp() {
		return remoteIp;
	}
	
	public boolean isLtpaTokenSet() {
		return ltpaTokenSet;
	}
	public void setLtpaTokenSet( boolean set) {
		this.ltpaTokenSet = set;
	}
	
	private void log(String message) {
		if (config.isDebug()) {
			System.out.println("(rememberMe) " + message);
		}
	}
	
	public boolean isAllowedIp() {
		return allowedIp;
	}
	

}
