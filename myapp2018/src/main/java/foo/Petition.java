package foo;

public class Petition {
	public String title;
	public String description;
	public String creator;
	
	public Boolean testValid() {
		return this.title != null && this.description != null && this.creator != null ;
	}
}
